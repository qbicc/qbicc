package cc.quarkus.qcc.graph;

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
        return index == 0 ? BlockLabel.getTargetOf(resumeTargetLabel) : catchMapper.getCatch(index - 1).getPinnedBlock();
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
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

            public PhiValue getCatch(final int index) {
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
         * Get the catch block with the given index.
         *
         * @param index the index
         * @return the catch block phi (must not be {@code null})
         */
        PhiValue getCatch(int index);

        interface Delegating extends CatchMapper {
            CatchMapper getDelegate();

            default int getCatchCount() {
                return getDelegate().getCatchCount();
            }

            default ClassTypeIdLiteral getCatchType(int index) {
                return getDelegate().getCatchType(index);
            }

            default PhiValue getCatch(int index) {
                return getDelegate().getCatch(index);
            }
        }
    }
}
