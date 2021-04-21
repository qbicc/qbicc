package org.qbicc.plugin.correctness;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.driver.Phase;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockEarlyTermination;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.CheckCast;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.ElementOf;
import org.qbicc.graph.InstanceFieldOf;
import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.ReferenceHandle;
import org.qbicc.graph.StaticField;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.ValueHandleVisitor;
import org.qbicc.graph.literal.IntegerLiteral;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.plugin.intrinsics.Intrinsics;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ArrayType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ReferenceArrayObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.SignedIntegerType;
import org.qbicc.type.UnsignedIntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

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

    public RuntimeChecksBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value load(ValueHandle handle, MemoryAtomicityMode mode) {
        check(handle);
        return super.load(handle, mode);
    }

    @Override
    public Node store(ValueHandle handle, Value value, MemoryAtomicityMode mode) {
        Value narrowedValue = check(handle, value);
        return super.store(handle, narrowedValue != null ? narrowedValue : value, mode);
    }

    @Override
    public Value typeIdOf(ValueHandle handle) {
        check(handle);
        return super.typeIdOf(handle);
    }

    @Override
    public Value addressOf(ValueHandle handle) {
        check(handle);
        return super.addressOf(handle);
    }

    @Override
    public Value arrayLength(ValueHandle handle) {
        check(handle);
        return super.arrayLength(handle);
    }

    @Override
    public Node invokeStatic(MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE) &&
            null == Intrinsics.get(ctxt).getStaticIntrinsic(Phase.LOWER, target.getEnclosingType().getDescriptor(), target.getName(), target.getDescriptor())) {
            throwUnsatisfiedLinkError();
            return nop();
        }
        if (target.isVirtual()) {
            throwIncompatibleClassChangeError();
            return nop();
        }
        return super.invokeStatic(target, arguments);
    }

    @Override
    public Value invokeValueStatic(MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE) &&
            null == Intrinsics.get(ctxt).getStaticValueIntrinsic(Phase.LOWER, target.getEnclosingType().getDescriptor(), target.getName(), target.getDescriptor())) {
            throwUnsatisfiedLinkError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
        }
        if (target.isVirtual()) {
            throwIncompatibleClassChangeError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
        }
        return super.invokeValueStatic(target, arguments);
    }

    @Override
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE) &&
            null == Intrinsics.get(ctxt).getInstanceIntrinsic(Phase.LOWER, target.getEnclosingType().getDescriptor(), target.getName(), target.getDescriptor())) {
            throwUnsatisfiedLinkError();
            return nop();
        }
        if (target.isStatic()) {
            throwIncompatibleClassChangeError();
            return nop();
        }
        nullCheck(instance);
        return super.invokeInstance(kind, instance, target, arguments);
    }

    @Override
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
        nullCheck(instance);
        return super.invokeInstance(kind, instance, owner, name, descriptor, arguments);
    }

    @Override
    public Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE) &&
            null == Intrinsics.get(ctxt).getInstanceValueIntrinsic(Phase.LOWER, target.getEnclosingType().getDescriptor(), target.getName(), target.getDescriptor())) {
            throwUnsatisfiedLinkError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
        }
        if (target.isStatic()) {
            throwIncompatibleClassChangeError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
        }
        nullCheck(instance);
        return super.invokeValueInstance(kind, instance, target, arguments);
    }

    @Override
    public Value invokeValueInstance(DispatchInvocation.Kind kind, Value instance, TypeDescriptor owner, String name, MethodDescriptor descriptor, List<Value> arguments) {
        nullCheck(instance);
        return super.invokeValueInstance(kind, instance, owner, name, descriptor, arguments);
    }

    @Override
    public Value invokeConstructor(Value instance, ConstructorElement target, List<Value> arguments) {
        nullCheck(instance);
        if (target.isStatic()) {
            throwIncompatibleClassChangeError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
        }
        return super.invokeConstructor(instance, target, arguments);
    }

    @Override
    public Value invokeConstructor(Value instance, TypeDescriptor owner, MethodDescriptor descriptor, List<Value> arguments) {
        nullCheck(instance);
        return super.invokeConstructor(instance, owner, descriptor, arguments);
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
    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        arraySizeCheck(size);
        return super.newArray(arrayType, size);
    }

    @Override
    public BasicBlock throw_(Value value) {
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
        ValueType v1Type = v1.getType();
        ValueType v2Type = v2.getType();
        if (v1Type instanceof IntegerType && v2Type instanceof IntegerType) {
            LiteralFactory lf = ctxt.getLiteralFactory();
            final IntegerLiteral zero = lf.literalOf(0);
            final BlockLabel throwIt = new BlockLabel();
            final BlockLabel goAhead = new BlockLabel();

            if_(isEq(v2, zero), throwIt, goAhead);
            try {
                begin(throwIt);
                MethodElement helper = ctxt.getVMHelperMethod("raiseArithmeticException");
                invokeStatic(helper, List.of());
                unreachable();
            } catch (BlockEarlyTermination ignored) {
                // continue
            }
            begin(goAhead);
        }
        return super.divide(v1, v2);
    }

    private void throwUnsatisfiedLinkError() {
        MethodElement helper = ctxt.getVMHelperMethod("raiseUnsatisfiedLinkError");
        invokeStatic(helper, List.of());
        throw new BlockEarlyTermination(unreachable());
    }

    private void throwIncompatibleClassChangeError() {
        MethodElement helper = ctxt.getVMHelperMethod("raiseIncompatibleClassChangeError");
        invokeStatic(helper, List.of());
        throw new BlockEarlyTermination(unreachable());
    }

    private Value check(ValueHandle handle) {
        return check(handle, null);
    }

    private Value check(ValueHandle handle, Value storedValue) {
        return handle.accept(new ValueHandleVisitor<Void, Value>() {
            @Override
            public Value visit(Void param, ElementOf node) {
                ValueHandle arrayHandle = node.getValueHandle();
                arrayHandle.accept(this, param);
                ValueType arrayType = arrayHandle.getValueType();
                if (arrayType instanceof ArrayObjectType) {
                    indexOutOfBoundsCheck(arrayHandle, node.getIndex());
                    if (arrayType instanceof ReferenceArrayObjectType && storedValue != null) {
                        Value toTypeId = load(instanceFieldOf(arrayHandle, Layout.get(ctxt).getRefArrayElementTypeIdField()), MemoryAtomicityMode.UNORDERED);
                        Value toDimensions = ctxt.getLiteralFactory().literalOf(((ReferenceArrayObjectType) arrayType).getDimensionCount() - 1);
                        return checkcast(storedValue, toTypeId, toDimensions, CheckCast.CastType.ArrayStore, ((ReferenceArrayObjectType) arrayType).getElementType());
                    }
                }
                return null;
            }

            @Override
            public Value visit(Void param, InstanceFieldOf node) {
                if (node.getVariableElement().isStatic()) {
                    throwIncompatibleClassChangeError();
                } else {
                    node.getValueHandle().accept(this, param);
                }
                return null;
            }

            @Override
            public Value visit(Void param, StaticField node) {
                if (! node.getVariableElement().isStatic()) {
                    throwIncompatibleClassChangeError();
                }
                return null;
            }

            @Override
            public Value visit(Void param, ReferenceHandle node) {
                nullCheck(node.getReferenceValue());
                return null;
            }
        }, null);
    }

    private void nullCheck(Value value) {
        if (value.getType() instanceof ArrayType || value.getType() instanceof ReferenceType && ! ((ReferenceType) value.getType()).isNullable()) {
            return;
        }

        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final LiteralFactory lf = ctxt.getLiteralFactory();
        if_(isEq(value, lf.zeroInitializerLiteralOfType(value.getType())), throwIt, goAhead);
        try {
            begin(throwIt);
            MethodElement helper = ctxt.getVMHelperMethod("raiseNullPointerException");
            invokeStatic(helper, List.of());
            unreachable();
        } catch (BlockEarlyTermination ignored) {
            //continue
        }
        begin(goAhead);
    }

    private void indexOutOfBoundsCheck(ValueHandle array, Value index) {
        final BlockLabel notNegative = new BlockLabel();
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral zero = lf.literalOf(0);

        if_(isLt(index, zero), throwIt, notNegative);
        try {
            begin(notNegative);
            final Value length = arrayLength(array);
            if_(isGe(index, length), throwIt, goAhead);
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        try {
            begin(throwIt);
            MethodElement helper = ctxt.getVMHelperMethod("raiseArrayIndexOutOfBoundsException");
            invokeStatic(helper, List.of());
            unreachable();
        } catch (BlockEarlyTermination ignored) {
            // continue
        }
        begin(goAhead);
    }

    private void arraySizeCheck(Value size) {
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final IntegerLiteral zero = ctxt.getLiteralFactory().literalOf(0);
        if_(isLt(size, zero), throwIt, goAhead);
        try {
            begin(throwIt);
            MethodElement helper = ctxt.getVMHelperMethod("raiseNegativeArraySizeException");
            invokeStatic(helper, List.of());
            unreachable();
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
