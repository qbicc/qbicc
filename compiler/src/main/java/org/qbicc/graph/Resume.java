package org.qbicc.graph;

/**
 * A terminator which designates a subsequent block for normal execution.
 */
public interface Resume extends Terminator {
    BlockLabel getResumeTargetLabel();

    default BasicBlock getResumeTarget() {
        return BlockLabel.requireTargetOf(getResumeTargetLabel());
    }
}
