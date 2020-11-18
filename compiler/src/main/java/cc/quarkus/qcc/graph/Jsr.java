package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.graph.literal.BlockLiteral;

/**
 *
 */
public final class Jsr extends AbstractNode implements Resume, Terminator {
    private final Node dependency;
    private final BlockLabel jsrTargetLabel;
    private final BlockLiteral returnAddress;

    Jsr(final int line, final int bci, final Node dependency, final BlockLabel jsrTargetLabel, final BlockLiteral returnAddress) {
        super(line, bci);
        this.dependency = dependency;
        this.jsrTargetLabel = jsrTargetLabel;
        this.returnAddress = returnAddress;
    }

    public BlockLabel getJsrTargetLabel() {
        return jsrTargetLabel;
    }

    public BasicBlock getJsrTarget() {
        return BlockLabel.getTargetOf(jsrTargetLabel);
    }

    public BlockLabel getResumeTargetLabel() {
        return returnAddress.getBlockLabel();
    }

    public Value getReturnAddressValue() {
        return returnAddress;
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
}
