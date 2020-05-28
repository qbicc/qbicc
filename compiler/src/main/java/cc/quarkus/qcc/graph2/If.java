package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface If extends Terminator {
    Value getCondition();
    void setCondition(Value cond);

    BasicBlock getTrueBranch();
    void setTrueBranch(BasicBlock branch);
    BasicBlock getFalseBranch();
    void setFalseBranch(BasicBlock branch);
}
