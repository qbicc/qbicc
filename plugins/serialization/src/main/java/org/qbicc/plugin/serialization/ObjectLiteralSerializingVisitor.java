package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ObjectLiteral;
import org.qbicc.graph.literal.ProgramObjectLiteral;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.interpreter.VmString;

/**
 * A visitor that finds object literals, serializes them to the initial heap
 * and replaces the object literal with a reference to the data declaration
 * in the initial heap.
 */
public class ObjectLiteralSerializingVisitor implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public ObjectLiteralSerializingVisitor(final CompilationContext ctxt, final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    public Value visit(final Node.Copier param, final StringLiteral node) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
        VmString vString = ctxt.getVm().intern(node.getValue());
        bth.serializeVmObject(vString);
        Literal literal = bth.referToSerializedVmObject(vString, node.getType(), ctxt.getOrAddProgramModule(param.getBlockBuilder().getRootElement()));
        return param.getBlockBuilder().notNull(literal);
    }

    public Value visit(final Node.Copier param, final ObjectLiteral node) {
        BuildtimeHeap bth = BuildtimeHeap.get(ctxt);
        bth.serializeVmObject(node.getValue());
        Literal literal = bth.referToSerializedVmObject(node.getValue(), node.getType(), ctxt.getOrAddProgramModule(param.getBlockBuilder().getRootElement()));
        return param.getBlockBuilder().notNull(literal);
    }
}
