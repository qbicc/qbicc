package cc.quarkus.qcc.graph;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public interface Resume extends Terminator {
    BlockLabel getResumeTargetLabel();

    default BasicBlock getResumeTarget() {
        return BlockLabel.getTargetOf(getResumeTargetLabel());
    }
}
