package org.qbicc.plugin.lowering;

import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.CmpAndSwap;
import org.qbicc.graph.GetAndBitwiseAnd;
import org.qbicc.graph.GetAndBitwiseNand;
import org.qbicc.graph.GetAndBitwiseOr;
import org.qbicc.graph.GetAndBitwiseXor;
import org.qbicc.graph.GetAndSet;
import org.qbicc.graph.Load;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Store;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.BooleanType;
import org.qbicc.type.CompoundType;
import org.qbicc.type.IntegerType;

/**
 *
 */
public final class BooleanAccessCopier implements NodeVisitor.Delegating<Node.Copier, Value, Node, BasicBlock, ValueHandle> {
    private final CompilationContext ctxt;
    private final NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate;

    public BooleanAccessCopier(CompilationContext ctxt, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> delegate) {
        this.ctxt = ctxt;
        this.delegate = delegate;
    }

    @Override
    public NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle> getDelegateNodeVisitor() {
        return delegate;
    }

    @Override
    public Node visit(Node.Copier param, Store node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedValue = param.copyValue(node.getValue());
        if (origHandle.getValueType() instanceof BooleanType && copyHandle.getValueType() instanceof IntegerType it) {
            copiedValue = b.extend(copiedValue, it);
        }
        return b.store(copyHandle, copiedValue, node.getMode());
    }

    @Override
    public Value visit(Node.Copier param, Load node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value loaded = b.load(copyHandle, node.getMode());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType) {
            return b.truncate(loaded, bt);
        } else {
            return loaded;
        }
    }

    @Override
    public Value visit(Node.Copier param, CmpAndSwap node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        Value copiedExpect = param.copyValue(node.getExpectedValue());
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        BasicBlockBuilder b = param.getBlockBuilder();
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            Value result = b.cmpAndSwap(copyHandle, b.extend(copiedExpect, it), b.extend(copiedUpdate, it), node.getSuccessAtomicityMode(), node.getFailureAtomicityMode(), node.getStrength());
            // the result is a { i8, i1 } if the field is boolean
            // we need to change to a { i1, i1 }
            LiteralFactory lf = ctxt.getLiteralFactory();
            CompoundType origType = CmpAndSwap.getResultType(ctxt, it);
            CompoundType newType = CmpAndSwap.getResultType(ctxt, bt);
            Value resultByteVal = b.extractMember(result, origType.getMember(0));
            Value resultFlag = b.extractMember(result, origType.getMember(1));
            result = b.insertMember(lf.zeroInitializerLiteralOfType(newType), newType.getMember(0), b.truncate(resultByteVal, bt));
            result = b.insertMember(result, newType.getMember(1), resultFlag);
            return result;
        } else {
            return b.cmpAndSwap(copyHandle, copiedExpect, copiedUpdate, node.getSuccessAtomicityMode(), node.getFailureAtomicityMode(), node.getStrength());
        }
    }

    @Override
    public Value visit(Node.Copier param, GetAndBitwiseAnd node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            return b.truncate(b.getAndBitwiseAnd(copyHandle, b.extend(copiedUpdate, it), node.getAtomicityMode()), bt);
        } else {
            return b.getAndBitwiseAnd(copyHandle, copiedUpdate, node.getAtomicityMode());
        }
    }

    @Override
    public Value visit(Node.Copier param, GetAndBitwiseNand node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            return b.truncate(b.getAndBitwiseNand(copyHandle, b.extend(copiedUpdate, it), node.getAtomicityMode()), bt);
        } else {
            return b.getAndBitwiseNand(copyHandle, copiedUpdate, node.getAtomicityMode());
        }
    }

    @Override
    public Value visit(Node.Copier param, GetAndBitwiseOr node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            return b.truncate(b.getAndBitwiseOr(copyHandle, b.extend(copiedUpdate, it), node.getAtomicityMode()), bt);
        } else {
            return b.getAndBitwiseOr(copyHandle, copiedUpdate, node.getAtomicityMode());
        }
    }

    @Override
    public Value visit(Node.Copier param, GetAndBitwiseXor node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            return b.truncate(b.getAndBitwiseXor(copyHandle, b.extend(copiedUpdate, it), node.getAtomicityMode()), bt);
        } else {
            return b.getAndBitwiseXor(copyHandle, copiedUpdate, node.getAtomicityMode());
        }
    }

    @Override
    public Value visit(Node.Copier param, GetAndSet node) {
        param.copyNode(node.getDependency());
        ValueHandle origHandle = node.getValueHandle();
        ValueHandle copyHandle = param.copyValueHandle(origHandle);
        BasicBlockBuilder b = param.getBlockBuilder();
        Value copiedUpdate = param.copyValue(node.getUpdateValue());
        if (origHandle.getValueType() instanceof BooleanType bt && copyHandle.getValueType() instanceof IntegerType it) {
            return b.truncate(b.getAndSet(copyHandle, b.extend(copiedUpdate, it), node.getAtomicityMode()), bt);
        } else {
            return b.getAndSet(copyHandle, copiedUpdate, node.getAtomicityMode());
        }
    }
}
