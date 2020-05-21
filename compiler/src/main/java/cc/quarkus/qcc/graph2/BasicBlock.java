package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface BasicBlock extends Node {
    TerminalInstruction getTerminalInstruction();
    void setTerminalInstruction(TerminalInstruction terminalInstruction);
}
