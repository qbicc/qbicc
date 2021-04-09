package org.qbicc.plugin.verification;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.BlockLabel;
import org.qbicc.graph.DelegatingBasicBlockBuilder;
import org.qbicc.graph.DispatchInvocation;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.BlockLiteral;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;

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
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
    }

    public Value invokeValueInstance(final DispatchInvocation.Kind kind, final Value instance, final MethodElement target, final List<Value> arguments) {
        invalidNode("invokeValueInstance");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(target.getType().getReturnType());
    }

    public Value invokeConstructor(final Value instance, final ConstructorElement target, final List<Value> arguments) {
        invalidNode("invokeConstructor");
        return instance;
    }

    public Value new_(final ClassObjectType type) {
        invalidNode("new");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(type.getReference());
    }

    public Value newArray(final ArrayObjectType arrayType, final Value size) {
        invalidNode("newArray");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(arrayType.getReference());
    }

    public Value multiNewArray(final ArrayObjectType arrayType, final List<Value> dimensions) {
        invalidNode("multiNewArray");
        return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(arrayType.getReference());
    }

    public Value typeIdOf(ValueHandle valueHandle) {
        invalidNode("typeIdOf");
        return ctxt.getLiteralFactory().literalOf(0);
    }

    private void invalidNode(String name) {
        ctxt.warning(getLocation(), "Invalid node encountered (cannot directly lower %s)", name);
    }
}
