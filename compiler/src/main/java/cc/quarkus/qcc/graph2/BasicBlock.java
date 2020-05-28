package cc.quarkus.qcc.graph2;

import java.util.Set;

/**
 *
 */
public interface BasicBlock extends Node {
    Terminator getTerminator();
    void setTerminator(Terminator terminator);

    Set<BasicBlock> calculateReachableBlocks();
}
