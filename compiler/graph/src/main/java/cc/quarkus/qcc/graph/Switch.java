package cc.quarkus.qcc.graph;

/**
 *
 */
public interface Switch extends Terminator {
    Value getSwitchValue();
    void setSwitchValue(Value value);

    BasicBlock getDefaultTarget();
    void setDefaultTarget(BasicBlock target);

    BasicBlock getTargetForValue(int value);
    void setTargetForValue(int value, BasicBlock target);

    int getNumberOfValues();

    int getValue(int index) throws IndexOutOfBoundsException;

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return getSwitchValue();
    }

    float getDensity();
}
