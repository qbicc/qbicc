package org.qbicc.graph;

/**
 * A node that can catch exceptions.
 */
public interface CatchNode extends Terminator {
    BlockLabel getCatchLabel();

    default BasicBlock getCatchBlock() {
        return BlockLabel.requireTargetOf(getCatchLabel());
    }
}
