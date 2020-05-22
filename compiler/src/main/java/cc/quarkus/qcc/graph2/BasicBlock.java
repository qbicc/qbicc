package cc.quarkus.qcc.graph2;

import java.util.Set;

/**
 *
 */
public interface BasicBlock extends Node {
    TerminalInstruction getTerminalInstruction();
    void setTerminalInstruction(TerminalInstruction terminalInstruction);

    Set<BasicBlock> getReachableBlocks();
}
