package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.AddressOf;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.ConstructorElementHandle;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.ExactMethodElementHandle;
import org.qbicc.graph.InstanceMethodElementHandle;
import org.qbicc.graph.InterfaceMethodElementHandle;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.PointerHandle;
import org.qbicc.graph.Slot;
import org.qbicc.graph.StaticMethodElementHandle;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.VirtualMethodElementHandle;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.TypeLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.RuntimeMethodFinder;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PrimitiveArrayObjectType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InstanceFieldElement;
import org.qbicc.type.definition.element.MethodElement;

/**
 * This builder injects runtime checks and transformations to:
 * <ul>
 *     <li>Throw {@link NullPointerException} if a {@code null} method, array, object gets dereferenced.</li>
 *     <li>Throw {@link IndexOutOfBoundsException} if an array index is out of bounds.</li>
 *     <li>Throw {@link ArithmeticException} when the divisor in an integer division is zero.</li>
 *     <li>Mask shift distances in {@code *shl}, {@code *shr}, and {@code *ushr}.</li>
 * </ul>
 */
public class RuntimeChecksBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public RuntimeChecksBasicBlockBuilder(final FactoryContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = getContext();
    }

    @Override
    public Value load(Value pointer, ReadAccessMode accessMode) {
        checkPointerValue(pointer, null);
        return super.load(pointer, accessMode);
    }

    @Override
    public Node store(Value handle, Value value, WriteAccessMode accessMode) {
        Value narrowedValue = checkPointerValue(handle, value);
        return super.store(handle, narrowedValue != null ? narrowedValue : value, accessMode);
    }

    @Override
    public Value checkcast(Value value, Value toType, Value toDimensions, CheckCast.CastType kind, ObjectType expectedType) {
        ValueType rawValueType = value.getType();
        if (value instanceof Dereference deref) {
            BasicBlockBuilder fb = getFirstBuilder();
            return fb.deref(fb.checkcast(deref.getPointer(), toType, toDimensions, kind, expectedType));
        } else if (value instanceof DecodeReference dr) {
            BasicBlockBuilder fb = getFirstBuilder();
            return fb.decodeReference(fb.checkcast(dr.getInput(), toType, toDimensions, kind, expectedType));
        }
        if (rawValueType instanceof ReferenceType refType) {
            ReferenceType outputType = refType.narrow(expectedType);
            if (outputType == null) {
                // impossible cast
                if (isNoThrow()) {
                    ctxt.error(getLocation(), "Narrowing %s to %s will always fail in no-throw element", rawValueType, expectedType);
                    throw new BlockEarlyTermination(unreachable());
                } else {
                    ctxt.warning(getLocation(), "Narrowing %s to %s will always fail", rawValueType, expectedType);
                    throwClassCastException();
                }
            } else {
                if (isNoThrow()) {
                    return bitCast(value, outputType);
                }
            }
            if (toType instanceof TypeLiteral tl
                && tl.getValue() instanceof ObjectType ot
                && toDimensions instanceof IntegerLiteral il && il.intValue() == 0
                && refType.instanceOf(ot)
            ) {
                // statically pass the test
                return value;
            }
        }
        return super.checkcast(value, toType, toDimensions, kind, expectedType);
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        check(handle);
        return super.addressOf(handle);
    }

    @Override
    public Value call(ValueHandle target, List<Value> arguments) {
        check(target);
        return super.call(target, arguments);
    }

    @Override
    public Value callNoSideEffects(ValueHandle target, List<Value> arguments) {
        check(target);
        return super.callNoSideEffects(target, arguments);
    }

    @Override
    public BasicBlock callNoReturn(ValueHandle target, List<Value> arguments) {
        check(target);
        return super.callNoReturn(target, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        check(target);
        return super.invokeNoReturn(target, arguments, catchLabel, targetArguments);
    }

    @Override
    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        return super.new_(type, typeId, size, align);
    }

    @Override
    public BasicBlock tailCall(ValueHandle target, List<Value> arguments) {
        check(target);
        return super.tailCall(target, arguments);
    }

    @Override
    public Value invoke(ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        check(target);
        return super.invoke(target, arguments, catchLabel, resumeLabel, targetArguments);
    }

    @Override
    public Node monitorEnter(Value obj) {
        nullCheck(obj);
        return super.monitorEnter(obj);
    }

    @Override
    public Node monitorExit(Value obj) {
        nullCheck(obj);
        return super.monitorExit(obj);
    }

    @Override
    public Value newArray(final PrimitiveArrayObjectType arrayType, final Value size) {
        arraySizeCheck(size);
        return super.newArray(arrayType, size);
    }

    @Override
    public Value newReferenceArray(final ReferenceArrayObjectType arrayType, Value elemTypeId, Value dimensions, final Value size) {
        arraySizeCheck(size);
        return super.newReferenceArray(arrayType, elemTypeId, dimensions, size);
    }

    @Override
    public BasicBlock throw_(Value value) {
        if (isNoThrow()) {
            ctxt.error(getLocation(), "Throw in no-throw element");
            return unreachable();
        }
        nullCheck(value);
        return super.throw_(value);
    }

    @Override
    public Value shl(Value v1, Value v2) {
        return super.shl(v1, castAndMaskShiftDistance(v1, v2));
    }

    @Override
    public Value shr(Value v1, Value v2) {
        return super.shr(v1, castAndMaskShiftDistance(v1, v2));
    }

    @Override
    public Value divide(Value v1, Value v2) {
        if (isNoThrow()) {
            if (v2.isDefEq(getLiteralFactory().zeroInitializerLiteralOfType(v2.getType()))) {
                ctxt.error(getLocation(), "Division by zero in no-throw element");
                throw new BlockEarlyTermination(unreachable());
            } else {
                return super.divide(v1, v2);
            }
        }
        ValueType v1Type = v1.getType();
        ValueType v2Type = v2.getType();
        if (v1Type instanceof IntegerType && v2Type instanceof IntegerType) {
            LiteralFactory lf = ctxt.getLiteralFactory();
            final IntegerLiteral zero = lf.literalOf((IntegerType) v2.getType(), 0);
            final BlockLabel throwIt = new BlockLabel();
            final BlockLabel goAhead = new BlockLabel();

            if_(isEq(v2, zero), throwIt, goAhead, Map.of());
            try {
                begin(throwIt);
                MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseArithmeticException");
                callNoReturn(staticMethod(helper), List.of());
            } catch (BlockEarlyTermination ignored) {
                // continue
            }
            begin(goAhead);
        }
        return super.divide(v1, v2);
    }

    @Override
    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        nullCheck(instancePointer);
        return super.instanceFieldOf(instancePointer, field);
    }

    private void throwIncompatibleClassChangeError() {
        MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseIncompatibleClassChangeError");
        throw new BlockEarlyTermination(callNoReturn(staticMethod(helper), List.of()));
    }

    private void throwClassCastException() {
        MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
        throw new BlockEarlyTermination(callNoReturn(staticMethod(helper), List.of()));
    }

    private Value check(ValueHandle handle) {
        return check(handle, null);
    }

    private Value check(ValueHandle handle, Value storedValue) {
        return handle.accept(new ValueHandleVisitor<Void, Value>() {
            @Override
            public Value visit(Void param, PointerHandle node) {
                return checkPointerValue(node.getPointerValue(), storedValue);
            }

            @Override
            public Value visit(Void param, ConstructorElementHandle node) {
                ConstructorElement target = node.getExecutable();
                nullCheck(node.getInstance());
                if (target.isStatic()) {
                    throwIncompatibleClassChangeError();
                    throw Assert.unreachableCode();
                }
                // return value unused in this case
                return null;
            }

            @Override
            public Value visit(Void param, ExactMethodElementHandle node) {
                //maybe needs an initCheck?
                return visit(node);
            }

            @Override
            public Value visit(Void param, InterfaceMethodElementHandle node) {
                return visit(node);
            }

            @Override
            public Value visit(Void param, VirtualMethodElementHandle node) {
                return visit(node);
            }

            private Value visit(VirtualMethodElementHandle node) {
                MethodElement target = node.getExecutable();
                // Elides check for native method because we don't know the exact target (could be overridden).
                if (target.isStatic()) {
                    throwIncompatibleClassChangeError();
                    throw Assert.unreachableCode();
                }
                nullCheck(node.getInstance());
                // return value unused in this case
                return null;
            }

            private Value visit(InstanceMethodElementHandle node) {
                MethodElement target = node.getExecutable();
                if (target.isStatic()) {
                    throwIncompatibleClassChangeError();
                    throw Assert.unreachableCode();
                }
                nullCheck(node.getInstance());
                // return value unused in this case
                return null;
            }

            @Override
            public Value visit(Void param, StaticMethodElementHandle node) {
                MethodElement target = node.getExecutable();
                if (target.isVirtual()) {
                    throwIncompatibleClassChangeError();
                    throw Assert.unreachableCode();
                }
                // return value unused in this case
                return null;
            }
        }, null);
    }

    private Value checkPointerValue(Value pointerValue, final Value storedValue) {
        // temporary
        while (pointerValue instanceof AddressOf ao && ao.getValueHandle() instanceof PointerHandle ph) {
            pointerValue = ph.getPointerValue();
        }
        if (pointerValue instanceof DecodeReference dr) {
            nullCheck(dr.getInput());
        } else if (pointerValue instanceof ElementOf eo &&
            eo.getArrayPointer() instanceof DecodeReference dr &&
            dr.getPointeeType() instanceof ArrayObjectType arrayType) {
            indexOutOfBoundsCheck(dr, eo.getIndex());
            if (arrayType instanceof ReferenceArrayObjectType referenceArrayType && storedValue != null) {
                Value toTypeId = load(instanceFieldOf(pointerHandle(dr), CoreClasses.get(ctxt).getRefArrayElementTypeIdField()));
                Value toDimensions;
                ObjectType jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getObjectType();
                if (referenceArrayType.getLeafElementType().equals(jlo)) {
                    // All arrays are subtypes of Object, so if the leafElementType is Object, we won't know the real dimension count until runtime!
                    toDimensions = sub(load(instanceFieldOf(pointerHandle(dr), CoreClasses.get(ctxt).getRefArrayDimensionsField())), ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), 1));
                } else {
                    toDimensions = ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), referenceArrayType.getDimensionCount() - 1);
                }
                return checkcast(storedValue, toTypeId, toDimensions, CheckCast.CastType.ArrayStore, referenceArrayType.getElementObjectType());
            }
        } else if (pointerValue instanceof StaticFieldLiteral sfl) {
            InitializerElement init = sfl.getVariableElement().getRunTimeInitializer();
            if (init != null && !getRootElement().equals(init)) {
                VmObject initThunkInstance = RuntimeInitManager.get(ctxt).getOnceInstance(init);
                initCheck(init, ctxt.getLiteralFactory().literalOf(initThunkInstance));
            }
        }
        return null;
    }

    /**
     * Determine whether the current element was declared "no throw".
     * Throwing an exception from a no-throw element will cause undefined behavior.
     * Therefore, any checks to avoid undefined behavior which rely on throwing exceptions are redundant in this situation.
     *
     * @return {@code true} if the current element is no-throw; {@code false} otherwise
     */
    private boolean isNoThrow() {
        return getCurrentElement().hasAllModifiersOf(ClassFile.I_ACC_NO_THROW);
    }

    private void nullCheck(Value value) {
        if (! (value.getType() instanceof ReferenceType) || ! value.isNullable() || isNoThrow()) {
            return;
        }

        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final LiteralFactory lf = ctxt.getLiteralFactory();
        if_(isEq(value, lf.zeroInitializerLiteralOfType(value.getType())), throwIt, goAhead, Map.of());
        try {
            begin(throwIt);
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseNullPointerException");
            callNoReturn(staticMethod(helper), List.of());
        } catch (BlockEarlyTermination ignored) {
            //continue
        }
        begin(goAhead);
    }

    private void arraySizeCheck(Value size) {
        if (isNoThrow()) {
            return;
        }
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final IntegerLiteral zero = ctxt.getLiteralFactory().literalOf(0);
        if_(isLt(size, zero), throwIt, goAhead, Map.of());
        try {
            begin(throwIt);
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseNegativeArraySizeException");
            callNoReturn(staticMethod(helper), List.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(goAhead);
    }

    private void indexOutOfBoundsCheck(DecodeReference dr, Value index) {
        if (isNoThrow()) {
            return;
        }
        if (index.isDefGe(getLiteralFactory().zeroInitializerLiteralOfType(index.getType()))) {
            if (dr.getInput() instanceof NewReferenceArray nra && index.isDefLt(nra.getSize())) {
                // no check needed; statically OK
                return;
            } else if (dr.getInput() instanceof MultiNewArray mna) {
                List<Value> dims = mna.getDimensions();
                if (index.isDefLt(dims.get(dims.size() - 1))) {
                    // no check needed; statically OK
                    return;
                }
            }
        }

        final BlockLabel notNegative = new BlockLabel();
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral zero = lf.literalOf(0);

        if_(isLt(index, zero), throwIt, notNegative, Map.of());
        try {
            begin(notNegative);
            final Value length = load(instanceFieldOf(pointerHandle(dr), CoreClasses.get(ctxt).getArrayLengthField()));
            if_(isGe(index, length), throwIt, goAhead, Map.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        try {
            begin(throwIt);
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseArrayIndexOutOfBoundsException");
            callNoReturn(staticMethod(helper), List.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(goAhead);
    }

    private Value castAndMaskShiftDistance(Value op, Value shiftDistance) {
        final ValueType opType = op.getType();

        // Correct shiftDistance's signedness to match that of op
        ValueType shiftType = shiftDistance.getType();
        if (opType instanceof SignedIntegerType && shiftType instanceof UnsignedIntegerType) {
            shiftDistance = bitCast(shiftDistance, ((UnsignedIntegerType) shiftType).asSigned());
        } else if (opType instanceof UnsignedIntegerType && shiftType instanceof SignedIntegerType) {
            shiftDistance = bitCast(shiftDistance, ((SignedIntegerType) shiftType).asUnsigned());
        }

        // Correct shiftDistance's size to match that of op
        shiftType = shiftDistance.getType();
        if (opType.getSize() < shiftType.getSize()) {
            shiftDistance = truncate(shiftDistance, (IntegerType) opType);
        } else if (opType.getSize() > shiftType.getSize()) {
            shiftDistance = extend(shiftDistance, (IntegerType) opType);
        }

        // Mask the shift distance
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final long bits = op.getType().getSize() * ctxt.getTypeSystem().getByteBits();
        shiftDistance = and(shiftDistance, lf.literalOf((IntegerType) opType, bits - 1));

        return shiftDistance;
    }
}
