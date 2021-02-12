package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

/**
 * A block cancellation is thrown by {@link BasicBlockBuilder} methods when a non-terminator method emits
 * a non-continuing terminator (for example when execution is short-circuited by an exception).
 */
@SuppressWarnings("serial")
public final class BlockEarlyTermination extends RuntimeException {
    private final BasicBlock terminatedBlock;

    public BlockEarlyTermination(BasicBlock terminatedBlock) {
        super("(cancelled block generation)", null, false, false);
        this.terminatedBlock = Assert.checkNotNullParam("terminatedBlock", terminatedBlock);
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }
}
