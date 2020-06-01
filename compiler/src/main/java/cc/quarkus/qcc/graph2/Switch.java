package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface Switch extends Terminator {
    BasicBlock getDefaultTarget();
    void setDefaultTarget(BasicBlock target);

    BasicBlock getTargetForValue(int value);
    void setTargetForValue(int value, BasicBlock target);

    int getNumberOfValues();

    int getValue(int index) throws IndexOutOfBoundsException;

    float getDensity();
}
