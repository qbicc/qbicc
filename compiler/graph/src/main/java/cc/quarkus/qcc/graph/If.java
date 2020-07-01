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

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getCondition() : Util.throwIndexOutOfBounds(index);
    }
}
