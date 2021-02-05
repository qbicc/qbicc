package cc.quarkus.qcc.plugin.correctness;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.JavaAccessMode;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.UnsignedIntegerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

import java.util.List;

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
    public Value typeIdOf(final Value value) {
        nullCheck(value);
        return super.typeIdOf(value);
    }

    @Override
    public Value readArrayValue(Value array, Value index, JavaAccessMode mode) {
        nullCheck(array);
        indexOutOfBoundsCheck(array, index);
        return super.readArrayValue(array, index, mode);
    }

    @Override
    public Node writeArrayValue(Value array, Value index, Value value, JavaAccessMode mode) {
        nullCheck(array);
        indexOutOfBoundsCheck(array, index);
        return super.writeArrayValue(array, index, value, mode);
    }

    @Override
    public Value arrayLength(Value array) {
        nullCheck(array);
        return super.arrayLength(array);
    }

    @Override
    public Value readInstanceField(Value instance, FieldElement fieldElement, JavaAccessMode mode) {
        nullCheck(instance);
        return super.readInstanceField(instance, fieldElement, mode);
    }

    @Override
    public Value readInstanceField(Value instance, TypeDescriptor owner, String name, TypeDescriptor descriptor, JavaAccessMode mode) {
        nullCheck(instance);
        return super.readInstanceField(instance, owner, name, descriptor, mode);
    }

    @Override
    public Node writeInstanceField(Value instance, FieldElement fieldElement, Value value, JavaAccessMode mode) {
        nullCheck(instance);
        return super.writeInstanceField(instance, fieldElement, value, mode);
    }

    @Override
    public Node writeInstanceField(Value instance, TypeDescriptor owner, String name, TypeDescriptor descriptor, Value value, JavaAccessMode mode) {
        nullCheck(instance);
        return super.writeInstanceField(instance, owner, name, descriptor, value, mode);
    }

    @Override
    public Node invokeStatic(MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            throwUnsatisfiedLinkError();
            return nop();
        }
        return super.invokeStatic(target, arguments);
    }

    @Override
    public Value invokeValueStatic(MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            throwUnsatisfiedLinkError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType(List.of()).getReturnType());
        }
        return super.invokeValueStatic(target, arguments);
    }

    @Override
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            throwUnsatisfiedLinkError();
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
        if (target.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            throwUnsatisfiedLinkError();
            return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType(List.of()).getReturnType());
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

            if_(cmpEq(v2, zero), throwIt, goAhead);
            begin(throwIt);
            ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
            ValidatedTypeDefinition ae = classContext.findDefinedType("java/lang/ArithmeticException").validate();
            Value ex = new_(ae.getClassType());
            ex = invokeConstructor(ex, ae.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
            throw_(ex); // Throw java.lang.ArithmeticException
            begin(goAhead);
        }
        return super.divide(v1, v2);
    }

    private void throwUnsatisfiedLinkError() {
        ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
        ValidatedTypeDefinition ule = classContext.findDefinedType("java/lang/UnsatisfiedLinkError").validate();
        BasicBlockBuilder builder = getFirstBuilder();
        Value ex = builder.new_(ule.getClassType());
        ex = builder.invokeConstructor(ex, ule.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        builder.throw_(ex); // Throw java.lang.UnsatisfiedLinkError
        begin(new BlockLabel());
    }

    private void nullCheck(Value value) {
        if (value.getType() instanceof ArrayType) {
            return;
        }

        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final NullLiteral nullLiteral = lf.literalOfNull();

        if_(cmpEq(value, nullLiteral), throwIt, goAhead);
        begin(throwIt);
        ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
        ValidatedTypeDefinition npe = classContext.findDefinedType("java/lang/NullPointerException").validate();
        Value ex = new_(npe.getClassType());
        ex = super.invokeConstructor(ex, npe.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        super.throw_(ex); // Throw java.lang.NullPointerException
        begin(goAhead);
    }

    private void indexOutOfBoundsCheck(Value array, Value index) {
        if (array.getType() instanceof ArrayType) {
            return;
        }

        final BlockLabel notNegative = new BlockLabel();
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();
        LiteralFactory lf = ctxt.getLiteralFactory();
        final IntegerLiteral zero = lf.literalOf(0);

        if_(cmpLt(index, zero), throwIt, notNegative);
        begin(notNegative);
        final Value length = arrayLength(array);
        if_(cmpGe(index, length), throwIt, goAhead);
        begin(throwIt);
        ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
        ValidatedTypeDefinition aiobe = classContext.findDefinedType("java/lang/ArrayIndexOutOfBoundsException").validate();
        Value ex = new_(aiobe.getClassType());
        ex = invokeConstructor(ex, aiobe.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        throw_(ex); // Throw java.lang.ArrayIndexOutOfBoundsException
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
