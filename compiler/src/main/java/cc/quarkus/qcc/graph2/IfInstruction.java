package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface IfInstruction extends TerminalInstruction {
    Value getCondition();
    void setCondition(Value cond);

    BasicBlock getTrueBranch();
    void setTrueBranch(BasicBlock branch);
    BasicBlock getFalseBranch();
    void setFalseBranch(BasicBlock branch);
}
