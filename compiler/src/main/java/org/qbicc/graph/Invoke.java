package org.qbicc.graph;

import java.util.List;
import java.util.Objects;

import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A catching method or function call.
 * The return value of the target is the type of this node's {@linkplain #getReturnValue() return value} (which may be {@link org.qbicc.type.VoidType VoidType}).
 * The return value node is always pinned to the resume block and thus is not accessible to the exception handler.
 * Exceptions thrown by the target are caught and delivered to the catch block.
 * If no exception is thrown by the callee, execution resumes in the resume block.
 * This node terminates its block.
 *
 * @see BasicBlockBuilder#invoke(org.qbicc.graph.ValueHandle, java.util.List, org.qbicc.graph.BlockLabel, org.qbicc.graph.BlockLabel)
 */
public final class Invoke extends AbstractTerminator implements Resume {
    private final Node dependency;
    private final BasicBlock terminatedBlock;
    private final ValueHandle target;
    private final List<Value> arguments;
    private final FunctionType functionType;
    private final BlockLabel catchLabel;
    private final BlockLabel resumeLabel;
    private final ReturnValue returnValue;

    Invoke(Node callSite, ExecutableElement element, int line, int bci, final BlockEntry blockEntry, Node dependency, ValueHandle target, List<Value> arguments, BlockLabel catchLabel, BlockLabel resumeLabel) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.terminatedBlock = new BasicBlock(blockEntry, this);
        this.target = target;
        this.arguments = arguments;
        this.catchLabel = catchLabel;
        this.resumeLabel = resumeLabel;
        functionType = (FunctionType) target.getValueType();
        returnValue = new ReturnValue();
    }

    @Override
    int calcHashCode() {
        return Objects.hash(Invoke.class, dependency, target, arguments);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Invoke && equals((Invoke) other);
    }

    public boolean equals(Invoke other) {
        return this == other || other != null && dependency.equals(other.dependency) && target.equals(other.target) && arguments.equals(other.arguments);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public FunctionType getFunctionType() {
        return functionType;
    }

    public ReturnValue getReturnValue() {
        return returnValue;
    }

    public List<Value> getArguments() {
        return arguments;
    }

    @Override
    public int getValueDependencyCount() {
        return arguments.size();
    }

    @Override
    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return arguments.get(index);
    }

    @Override
    public boolean hasValueHandleDependency() {
        return true;
    }

    @Override
    public ValueHandle getValueHandle() {
        return target;
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
        return 2;
    }

    @Override
    public BasicBlock getSuccessor(int index) {
        return index == 0 ? getResumeTarget() : index == 1 ? getCatchBlock() : Util.throwIndexOutOfBounds(index);
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
            super(Invoke.this.getCallSite(), Invoke.this.getElement(), Invoke.this.getSourceLine(), Invoke.this.getBytecodeIndex());
        }

        @Override
        int calcHashCode() {
            return Invoke.this.hashCode();
        }

        public Invoke getInvoke() {
            return Invoke.this;
        }

        @Override
        public ValueType getType() {
            return getFunctionType().getReturnType();
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

        public boolean equals(ReturnValue other) {
            return this == other || other != null && getInvoke().equals(other.getInvoke());
        }

        @Override
        public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
            return visitor.visit(param, this);
        }
    }
}
