package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface GotoInstruction extends TerminalInstruction {
    BasicBlock getTarget();
    void setTarget(BasicBlock branch);
}
