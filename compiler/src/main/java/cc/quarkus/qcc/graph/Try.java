package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;

/**
 * An operation which may throw an exception.
 */
public final class Try extends AbstractNode implements Resume {
    private final Triable delegateOperation;
    private final CatchMapper catchMapper;
    private final BlockLabel resumeTargetLabel;

    Try(final Triable delegateOperation, final CatchMapper catchMapper, final BlockLabel resumeTargetLabel) {
        super(delegateOperation.getSourceLine(), delegateOperation.getBytecodeIndex());
        this.delegateOperation = delegateOperation;
        this.catchMapper = catchMapper;
        this.resumeTargetLabel = resumeTargetLabel;
    }

    public Triable getDelegateOperation() {
        return delegateOperation;
    }

    public CatchMapper getCatchMapper() {
        return catchMapper;
    }

    public BlockLabel getResumeTargetLabel() {
        return resumeTargetLabel;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? delegateOperation : Util.throwIndexOutOfBounds(index);
    }

    public int getSuccessorCount() {
        return 1 + catchMapper.getCatchCount();
    }

    public BasicBlock getSuccessor(final int index) {
        return index == 0 ? BlockLabel.getTargetOf(resumeTargetLabel) : BlockLabel.getTargetOf(catchMapper.getCatchHandler(index - 1));
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Try.class, delegateOperation, catchMapper, resumeTargetLabel);
    }

    public boolean equals(final Object other) {
        return other instanceof Try && equals((Try) other);
    }

    public boolean equals(final Try other) {
        return this == other || other != null
            && delegateOperation.equals(other.delegateOperation)
            && catchMapper.equals(other.catchMapper)
            && resumeTargetLabel.equals(other.resumeTargetLabel);
    }

    /**
     * A mapper for try/catch, which returns the catch handler for a given throwable type.
     */
    public interface CatchMapper {
        /**
         * The catch mapper when there is no active {@code try} handler.
         */
        CatchMapper NONE = new CatchMapper() {
            public int getCatchCount() {
                return 0;
            }

            public ClassTypeIdLiteral getCatchType(final int index) {
                throw new IndexOutOfBoundsException();
            }

            public BlockLabel getCatchHandler(final int index) {
                throw new IndexOutOfBoundsException();
            }

            public void setCatchValue(final int index, final BasicBlock from, final Value value) {
                throw new IndexOutOfBoundsException();
            }
        };

        /**
         * Get the number of catch blocks for this catch mapper.  For a delegating catch mapper, this will
         * generally be one plus the number of the catch blocks of the delegate.
         *
         * @return the catch block count
         */
        int getCatchCount();

        /**
         * Get the type of the catch block with the given index.
         *
         * @param index the index
         * @return the type of the catch block
         */
        ClassTypeIdLiteral getCatchType(int index);

        /**
         * Get the block label of the handler with the given index.
         *
         * @param index the index
         * @return the catch handler (must not be {@code null})
         */
        BlockLabel getCatchHandler(int index);

        /**
         * Set the catch value to the given value {@code value} when the handler at index {@code index} is called
         * from block {@code from}.  The exception value may be a {@link Catch} or it may be a value that was thrown
         * by an immediate {@code throw}.  The associated {@link BasicBlockBuilder} must <em>not</em> have a current
         * block when this is called.
         *
         * @param index the index
         * @param from the from block
         * @param value the exception value
         */
        void setCatchValue(int index, BasicBlock from, Value value);

        interface Delegating extends CatchMapper {
            CatchMapper getDelegate();

            default int getCatchCount() {
                return getDelegate().getCatchCount();
            }

            default ClassTypeIdLiteral getCatchType(int index) {
                return getDelegate().getCatchType(index);
            }

            default BlockLabel getCatchHandler(int index) {
                return getDelegate().getCatchHandler(index);
            }

            default void setCatchValue(int index, BasicBlock from, Value value) {
                getDelegate().setCatchValue(index, from, value);
            }
        }
    }
}
