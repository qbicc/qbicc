package org.qbicc.plugin.correctness;

import java.util.List;
import java.util.Map;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DecodeReference;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.Dereference;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.MultiNewArray;
import org.qbicc.graph.NewReferenceArray;
import org.qbicc.graph.Node;
import org.qbicc.graph.Slot;
import org.qbicc.graph.Value;
import org.qbicc.graph.atomic.ReadAccessMode;
import org.qbicc.graph.atomic.WriteAccessMode;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.StaticFieldLiteral;
import org.qbicc.graph.literal.TypeIdLiteral;
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
            if (toType instanceof TypeIdLiteral tl
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
    public Value call(Value targetPtr, Value receiver, List<Value> arguments) {
        checkPointerValue(targetPtr, null);
        return super.call(targetPtr, receiver, arguments);
    }

    @Override
    public Value callNoSideEffects(Value targetPtr, Value receiver, List<Value> arguments) {
        checkPointerValue(targetPtr, null);
        return super.callNoSideEffects(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock callNoReturn(Value targetPtr, Value receiver, List<Value> arguments) {
        checkPointerValue(targetPtr, null);
        return super.callNoReturn(targetPtr, receiver, arguments);
    }

    @Override
    public BasicBlock invokeNoReturn(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        checkPointerValue(targetPtr, null);
        return super.invokeNoReturn(targetPtr, receiver, arguments, catchLabel, targetArguments);
    }

    @Override
    public Value new_(final ClassObjectType type, final Value typeId, final Value size, final Value align) {
        return super.new_(type, typeId, size, align);
    }

    @Override
    public BasicBlock tailCall(Value targetPtr, Value receiver, List<Value> arguments) {
        checkPointerValue(targetPtr, null);
        return super.tailCall(targetPtr, receiver, arguments);
    }

    @Override
    public Value invoke(Value targetPtr, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
        checkPointerValue(targetPtr, null);
        return super.invoke(targetPtr, receiver, arguments, catchLabel, resumeLabel, targetArguments);
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
    public Value instanceFieldOf(Value instancePointer, InstanceFieldElement field) {
        return super.instanceFieldOf(instancePointer, field);
    }

    private void throwClassCastException() {
        MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseClassCastException");
        throw new BlockEarlyTermination(callNoReturn(getLiteralFactory().literalOf(helper), List.of()));
    }

    private Value checkPointerValue(Value pointerValue, final Value storedValue) {
        if (pointerValue instanceof ElementOf eo &&
            eo.getArrayPointer() instanceof DecodeReference dr &&
            dr.getPointeeType() instanceof ArrayObjectType arrayType) {
            indexOutOfBoundsCheck(dr, eo.getIndex());
            if (arrayType instanceof ReferenceArrayObjectType referenceArrayType && storedValue != null) {
                Value toTypeId = load(instanceFieldOf(dr, CoreClasses.get(ctxt).getRefArrayElementTypeIdField()));
                Value toDimensions;
                ObjectType jlo = ctxt.getBootstrapClassContext().findDefinedType("java/lang/Object").load().getObjectType();
                if (referenceArrayType.getLeafElementType().equals(jlo)) {
                    // All arrays are subtypes of Object, so if the leafElementType is Object, we won't know the real dimension count until runtime!
                    toDimensions = sub(load(instanceFieldOf(dr, CoreClasses.get(ctxt).getRefArrayDimensionsField())), ctxt.getLiteralFactory().literalOf(ctxt.getTypeSystem().getUnsignedInteger8Type(), 1));
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

    @Override
    public Value nullCheck(Value value) {
        if (! isNoThrow() && value.isNullable() && value.getType() instanceof ReferenceType rt) {
            final BlockLabel throwIt = new BlockLabel();
            final BlockLabel goAhead = new BlockLabel();
            final LiteralFactory lf = ctxt.getLiteralFactory();
            final BasicBlockBuilder fb = getFirstBuilder();
            fb.begin(throwIt, bbb -> {
                MethodElement helper = RuntimeMethodFinder.get(getContext()).getMethod("raiseNullPointerException");
                bbb.callNoReturn(getLiteralFactory().literalOf(helper), List.of());
            });
            if_(isEq(value, lf.nullLiteralOfType(rt)), throwIt, goAhead, Map.of(Slot.result(), value));
            begin(goAhead);
            return notNull(addParam(goAhead, Slot.result(), rt, false));
        }
        return value;
    }

    @Override
    public Value divisorCheck(Value input) {
        if (input instanceof IntegerLiteral il && il.isNonZero()) {
            return il;
        }
        if (! isNoThrow() && input.getType() instanceof IntegerType it) {
            final LiteralFactory lf = getLiteralFactory();
            final IntegerLiteral zero = lf.literalOf(it, 0);
            final BlockLabel throwIt = new BlockLabel();
            final BasicBlockBuilder fb = getFirstBuilder();
            fb.begin(throwIt, bbb -> {
                MethodElement helper = RuntimeMethodFinder.get(getContext()).getMethod("raiseArithmeticException");
                bbb.callNoReturn(getLiteralFactory().literalOf(helper), List.of());
            });
            final BlockLabel goAhead = new BlockLabel();
            if_(isEq(input, zero), throwIt, goAhead, Map.of(Slot.result(), input));
            begin(goAhead);
            // todo: constrain output
            return addParam(goAhead, Slot.result(), it);
        }
        return input;
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
            callNoReturn(getLiteralFactory().literalOf(helper), List.of());
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
            final Value length = load(instanceFieldOf(dr, CoreClasses.get(ctxt).getArrayLengthField()));
            if_(isGe(index, length), throwIt, goAhead, Map.of());
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        try {
            begin(throwIt);
            MethodElement helper = RuntimeMethodFinder.get(ctxt).getMethod("raiseArrayIndexOutOfBoundsException");
            callNoReturn(getLiteralFactory().literalOf(helper), List.of());
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
