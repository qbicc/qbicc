package org.qbicc.graph;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ProgramLocatable;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.InvokableType;
import org.qbicc.type.ValueType;

/**
 * A catching method or function call.
 * The return value of the target is the type of this node's {@linkplain #getReturnValue() return value} (which may be {@link org.qbicc.type.VoidType VoidType}).
 * The return value node is always pinned to the resume block and thus is not accessible to the exception handler.
 * Exceptions thrown by the target are caught and delivered to the catch block.
 * If no exception is thrown by the callee, execution resumes in the resume block.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#invoke(org.qbicc.graph.Value, org.qbicc.graph.Value, java.util.List, org.qbicc.graph.BlockLabel, org.qbicc.graph.BlockLabel, java.util.Map)
 */
public final class Invoke extends AbstractTerminator implements Resume, InvocationNode, CatchNode {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final Value target;
    private final Value receiver;
    private final List<Value> arguments;
    private final InvokableType calleeType;
    private final BlockLabel catchLabel;
    private final BlockLabel resumeLabel;
    private final ReturnValue returnValue;

    Invoke(final ProgramLocatable pl, final BlockEntry blockEntry, Node dependency, Value target, Value receiver, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel, Map<Slot, Value> targetArguments) {
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
        this.resumeLabel = resumeLabel;
        calleeType = (InvokableType) target.getPointeeType();
        returnValue = new ReturnValue();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(Invoke.class, dependency, target, receiver, arguments);
    }

    @Override
    String getNodeName() {
        return "Invoke";
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Invoke && equals((Invoke) other);
    }

    public boolean equals(Invoke other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && receiver.equals(other.receiver) && arguments.equals(other.arguments);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return InvocationNode.toRValueString(this, "invoke", b).append(" catch ").append(catchLabel).append(" resume ").append(resumeLabel);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public boolean maySafePoint() {
        return target.safePointBehavior() != SafePointBehavior.FORBIDDEN;
    }

    public InvokableType getCalleeType() {
        return calleeType;
    }

    public ReturnValue getReturnValue() {
        return returnValue;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public Value getTarget() {
        return target;
    }

    @Override
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

    @Override
    public int getSuccessorCount() {
        return 2;
    }

    @Override
    public BasicBlock getSuccessor(int index) {
        return index == 0 ? getResumeTarget() : index == 1 ? getCatchBlock() : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public boolean isImplicitOutboundArgument(Slot slot, BasicBlock block) {
        return slot == Slot.thrown() && block == getCatchBlock() || slot == Slot.result() && block == getResumeTarget();
    }

    @Override
    public <T, R> R accept(TerminatorVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public BlockLabel getResumeTargetLabel() {
        return resumeLabel;
    }

    /**
     * The node representing the return value of an {@code invoke} operation.
     */
    public final class ReturnValue extends AbstractValue implements PinnedNode, OrderedNode {

        ReturnValue() {
            super(Invoke.this);
        }

        @Override
        int calcHashCode() {
            return Invoke.this.hashCode();
        }

        @Override
        String getNodeName() {
            return "ReturnValue";
        }

        public Invoke getInvoke() {
            return Invoke.this;
        }

        @Override
        public ValueType getType() {
            return getCalleeType().getReturnType();
        }

        @Override
        public BlockLabel getPinnedBlockLabel() {
            return Invoke.this.resumeLabel;
        }

        @Override
        public Node getDependency() {
            return getPinnedBlock().getBlockEntry();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ReturnValue && equals((ReturnValue) other);
        }

        @Override
        public int getScheduleIndex() {
            return Invoke.this.getScheduleIndex();
        }

        @Override
        public BasicBlock getScheduledBlock() {
            return Invoke.this.getScheduledBlock();
        }

        @Override
        public StringBuilder toString(StringBuilder b) {
            super.toString(b);
            b.append(" of ");
            Invoke.this.toString(b);
            return b;
        }

        public boolean equals(ReturnValue other) {
            return this == other || other != null && getInvoke().equals(other.getInvoke());
        }

        @Override
        public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
            return visitor.visit(param, this);
        }
    }
}
