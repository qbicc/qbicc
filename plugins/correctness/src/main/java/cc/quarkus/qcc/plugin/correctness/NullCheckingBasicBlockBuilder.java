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
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.NullLiteral;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.ValidatedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

import java.util.List;

/**
 * This builder introduces null checks for dereferences of methods, fields, and array accesses.
 */
public class NullCheckingBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public NullCheckingBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    @Override
    public Value readArrayValue(Value array, Value index, JavaAccessMode mode) {
        nullCheck(array);
        return super.readArrayValue(array, index, mode);
    }

    @Override
    public Node writeArrayValue(Value array, Value index, Value value, JavaAccessMode mode) {
        nullCheck(array);
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
    public Node invokeInstance(DispatchInvocation.Kind kind, Value instance, MethodElement target, List<Value> arguments) {
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

    private void nullCheck(Value value) {
        final LiteralFactory lf = ctxt.getLiteralFactory();
        final NullLiteral nullLiteral = lf.literalOfNull();
        final BlockLabel throwIt = new BlockLabel();
        final BlockLabel goAhead = new BlockLabel();

        if_(cmpEq(value, nullLiteral), throwIt, goAhead);
        begin(throwIt);
        ClassContext classContext = getCurrentElement().getEnclosingType().getContext();
        ValidatedTypeDefinition npe = classContext.findDefinedType("java/lang/NullPointerException").validate();
        Value ex = new_(npe.getClassType());
        ex = invokeConstructor(ex, npe.resolveConstructorElement(MethodDescriptor.VOID_METHOD_DESCRIPTOR), List.of());
        throw_(ex); // Throw java.lang.NullPointerException
        begin(goAhead);
    }
}
