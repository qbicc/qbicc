package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.graph.literal.BlockLiteral;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class Jsr extends AbstractNode implements Resume, Terminator {
    private final Node dependency;
    private final BlockLabel jsrTargetLabel;
    private final BlockLiteral returnAddress;
    private final BasicBlock terminatedBlock;

    Jsr(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final BlockLabel jsrTargetLabel, final BlockLiteral returnAddress) {
        super(callSite, element, line, bci);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.jsrTargetLabel = jsrTargetLabel;
        this.returnAddress = returnAddress;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
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

    int calcHashCode() {
        return Objects.hash(dependency, jsrTargetLabel, returnAddress);
    }

    public boolean equals(final Object other) {
        return other instanceof Jsr && equals((Jsr) other);
    }

    public boolean equals(final Jsr other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && jsrTargetLabel.equals(other.jsrTargetLabel)
            && returnAddress.equals(other.returnAddress);
    }
}
