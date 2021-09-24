package org.qbicc.plugin.serialization;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.StringLiteral;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmString;
import org.qbicc.object.Data;
import org.qbicc.object.Section;

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
        VmString vString = ctxt.getVm().intern(node.getValue());
        Data literal = BuildtimeHeap.get(ctxt).serializeVmObject(vString);

        Section section = ctxt.getImplicitSection(param.getBlockBuilder().getRootElement());
        section.declareData(null, literal.getName(), literal.getType()).setAddrspace(1);

        SymbolLiteral refToLiteral = ctxt.getLiteralFactory().literalOfSymbol(literal.getName(), literal.getType().getPointer().asCollected());
        return param.getBlockBuilder().notNull(ctxt.getLiteralFactory().bitcastLiteral(refToLiteral, node.getType()));
    }
}
