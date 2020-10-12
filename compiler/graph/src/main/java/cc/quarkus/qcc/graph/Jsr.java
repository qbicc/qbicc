package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Jsr extends AbstractNode implements Resume, Terminator {
    private final Node dependency;
    private final BlockLabel jsrTargetLabel;
    private final BlockLabel resumeTargetLabel;
    private final Value returnAddressValue;

    private Jsr(final Node dependency, final BlockLabel jsrTargetLabel, final BlockLabel resumeTargetLabel) {
        this.dependency = dependency;
        this.jsrTargetLabel = jsrTargetLabel;
        this.resumeTargetLabel = resumeTargetLabel;
        returnAddressValue = Value.const_(resumeTargetLabel);
    }

    public BlockLabel getJsrTargetLabel() {
        return jsrTargetLabel;
    }

    public BasicBlock getJsrTarget() {
        return BlockLabel.getTargetOf(jsrTargetLabel);
    }

    public BlockLabel getResumeTargetLabel() {
        return resumeTargetLabel;
    }

    public Value getReturnAddressValue() {
        return returnAddressValue;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getSuccessorCount() {
        return 2;
    }

    public BasicBlock getSuccessor(final int index) {
        return index == 0 ? getJsrTarget() : index == 1 ? getResumeTarget() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static void create(final GraphFactory.Context ctxt, BlockLabel targetLabel, BlockLabel returnLabel) {
        Jsr jsr = new Jsr(ctxt.getDependency(), targetLabel, returnLabel);
        ctxt.getCurrentBlock().setTarget(new BasicBlock(jsr));
        ctxt.setCurrentBlock(null);
    }
}
