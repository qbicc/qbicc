package cc.quarkus.qcc.graph;

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
