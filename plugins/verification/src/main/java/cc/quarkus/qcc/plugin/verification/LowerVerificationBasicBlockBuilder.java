package cc.quarkus.qcc.plugin.verification;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.BlockLabel;
import cc.quarkus.qcc.graph.DelegatingBasicBlockBuilder;
import cc.quarkus.qcc.graph.DispatchInvocation;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 * A block builder that forbids lowering of high-level (first phase) nodes in order to keep the back end(s) as simple
 * as possible.
 */
public class LowerVerificationBasicBlockBuilder extends DelegatingBasicBlockBuilder {
    private final CompilationContext ctxt;

    public LowerVerificationBasicBlockBuilder(final CompilationContext ctxt, final BasicBlockBuilder delegate) {
        super(delegate);
        this.ctxt = ctxt;
    }

    public BasicBlock throw_(final Value value) {
        invalidNode("throw");
        return return_();
    }

    public BasicBlock noSuchMethodError(final ObjectType owner, final MethodDescriptor desc, final String name) {
        invalidNode("noSuchMethodError");
        return return_();
    }

    public BasicBlock classNotFoundError(final String name) {
        invalidNode("classNotFoundError");
        return return_();
    }

    public BasicBlock jsr(final BlockLabel subLabel, final BlockLiteral returnAddress) {
        invalidNode("jsr");
        return goto_(returnAddress.getBlockLabel());
    }

    public BasicBlock ret(final Value address) {
        invalidNode("ret");
        return return_();
    }

    public Value clone(final Value object) {
        invalidNode("clone");
        return object;
    }

    public Node monitorEnter(final Value obj) {
        invalidNode("monitorEnter");
        return nop();
    }

    public Node monitorExit(final Value obj) {
        invalidNode("monitorExit");
        return nop();
    }

    public Node invokeStatic(final MethodElement target, final List<Value> arguments) {
        invalidNode("invokeStatic");
        return nop();
    }

    public Node invokeInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        invalidNode("invokeInstance");
        return nop();
    }

    public Value invokeValueStatic(final MethodElement target, final List<Value> arguments) {
        invalidNode("invokeValueStatic");
        return ctxt.getLiteralFactory().literalOfNull();
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        invalidNode("invokeValueInstance");
        return ctxt.getLiteralFactory().literalOfNull();
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        invalidNode("invokeConstructor");
        return instance;
    }

    public Value new_(final ClassObjectType type) {
        invalidNode("new");
        return super.new_(type);
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        invalidNode("new");
        return super.newArray(arrayType, size);
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        invalidNode("new");
        return super.multiNewArray(arrayType, dimensions);
    }

    private void invalidNode(String name) {
        ctxt.warning(getLocation(), "Invalid node encountered (cannot directly lower %s)", name);
    }
}
