package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.type.InvokableType;

/**
 * A catching method or function call which never returns normally.
 * Exceptions thrown by the target are caught and delivered to the catch block.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#invokeNoReturn(org.qbicc.graph.Value, org.qbicc.graph.Value, java.util.List, org.qbicc.graph.BlockLabel, java.util.Map)
 */
public final class InvokeNoReturn extends AbstractTerminator implements InvocationNode, CatchNode {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;
    private final InvokableType calleeType;
    private final BlockLabel catchLabel;

    InvokeNoReturn(final ProgramLocatable pl, final BlockEntry blockEntry, Node dependency, Value target, Value receiver, List<Value> arguments, BlockLabel catchLabel, Map<Slot, Value> targetArguments) {
        super(pl, targetArguments);
        for (int i = 0; i < arguments.size(); i++) {
            Assert.checkNotNullArrayParam("arguments", i, arguments.get(i));
        }
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.receiver = receiver;
        this.arguments = arguments;
        this.catchLabel = catchLabel;
        calleeType = (InvokableType) target.getPointeeType();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(InvokeNoReturn.class, dependency, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "InvokeNoReturn";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof InvokeNoReturn && equals((InvokeNoReturn) other);
    }

    public boolean equals(InvokeNoReturn other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "invoke", b).append(" no-return catch ").append(catchLabel);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return ! target.isNoSafePoints();
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    public Value getTarget() {
        return target;
    }

    public Value getReceiver() {
        return receiver;
    }

    @Override
    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public BlockLabel getCatchLabel() {
        return catchLabel;
    }

    public BasicBlock getCatchBlock() {
        return BlockLabel.getTargetOf(catchLabel);
    }

    @Override
    public int getSuccessorCount() {
        return 1;
    }

    @Override
    public BasicBlock getSuccessor(int index) {
        return index == 0 ? getCatchBlock() : Util.throwIndexOutOfBounds(index);
    }

    public boolean isImplicitOutboundArgument(final Slot slot, final BasicBlock block) {
        return slot == Slot.thrown() && block == getCatchBlock();
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
