package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.Load;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.ReadModifyWrite;
import org.qbicc.graph.Return;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.StructType;
import org.qbicc.type.IntegerType;

/**
 *
 */
public final class BooleanAccessCopier implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate;

    public BooleanAccessCopier(CompilationContext ctxt, NodeVisitor<Node.Copier, Value, Node, BasicBlock> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    @Override
    public NodeVisitor<Node.Copier, Value, Node, BasicBlock> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Node visit(Node.Copier param, Store node) {
        param.copyNode(node.getDependency());
        Value origPointer = node.getPointer();
        Value copyPointer = param.copyValue(origPointer);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedValue = param.copyValue(node.getValue());
        if (origPointer.getPointeeType() instanceof BooleanType && copyPointer.getPointeeType() instanceof IntegerType it) {
            copiedValue = b.extend(copiedValue, it);
        }
        return b.store(copyPointer, copiedValue, node.getAccessMode());
    }

    @Override
    public Value visit(Node.Copier param, Load node) {
        param.copyNode(node.getDependency());
        Value origPointer = node.getPointer();
        Value copyPointer = param.copyValue(origPointer);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value loaded = b.load(copyPointer, node.getAccessMode());
        if (origPointer.getPointeeType() instanceof BooleanType bt && copyPointer.getPointeeType() instanceof IntegerType) {
            return b.truncate(loaded, bt);
        } else {
            return loaded;
        }
    }

    @Override
    public Value visit(Node.Copier param, CmpAndSwap node) {
        param.copyNode(node.getDependency());
        Value origPointer = node.getPointer();
        Value copyPointer = param.copyValue(origPointer);
        Value copiedExpect = param.copyValue(node.getExpectedValue());
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        BasicBlockBuilder b = param.getBlockBuilder();
        if (origPointer.getPointeeType() instanceof BooleanType bt && copyPointer.getPointeeType() instanceof IntegerType it) {
            Value result = b.cmpAndSwap(copyPointer, b.extend(copiedExpect, it), b.extend(copiedUpdate, it), node.getReadAccessMode(), node.getWriteAccessMode(), node.getStrength());
            // the result is a { i8, i1 } if the field is boolean
            // we need to change to a { i1, i1 }
            LiteralFactory lf = ctxt.getLiteralFactory();
            StructType origType = CmpAndSwap.getResultType(ctxt, it);
            StructType newType = CmpAndSwap.getResultType(ctxt, bt);
            Value resultByteVal = b.extractMember(result, origType.getMember(0));
            Value resultFlag = b.extractMember(result, origType.getMember(1));
            result = b.insertMember(lf.zeroInitializerLiteralOfType(newType), newType.getMember(0), b.truncate(resultByteVal, bt));
            result = b.insertMember(result, newType.getMember(1), resultFlag);
            return result;
        } else {
            return b.cmpAndSwap(copyPointer, copiedExpect, copiedUpdate, node.getReadAccessMode(), node.getWriteAccessMode(), node.getStrength());
        }
    }

    @Override
    public Value visit(Node.Copier param, ReadModifyWrite node) {
        param.copyNode(node.getDependency());
        Value origPointer = node.getPointer();
        Value copyPointer = param.copyValue(origPointer);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        ReadModifyWrite.Op op = node.getOp();
        if (origPointer.getPointeeType() instanceof BooleanType bt && copyPointer.getPointeeType() instanceof IntegerType it) {
            return b.truncate(b.readModifyWrite(copyPointer, op, b.extend(copiedUpdate, it), node.getReadAccessMode(), node.getWriteAccessMode()), bt);
        } else {
            return b.readModifyWrite(copyPointer, op, copiedUpdate, node.getReadAccessMode(), node.getWriteAccessMode());
        }
    }

    @Override
    public BasicBlock visit(Node.Copier param, Return node) {
        BasicBlockBuilder b = param.getBlockBuilder();
        param.copyNode(node.getDependency());
        Value copiedReturnValue = param.copyValue(node.getReturnValue());

        if (node.getElement().getType().getReturnType() instanceof BooleanType bt   && copiedReturnValue.getType() instanceof IntegerType) {
            return b.return_(b.truncate(copiedReturnValue, bt));
        } else {
            return b.return_(copiedReturnValue);
        }
    }
}
