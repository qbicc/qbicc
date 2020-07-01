package cc.quarkus.qcc.graph;

/**
 *
 */
public interface BinaryValue extends Value, ProgramNode {
    Value getLeftInput();
    void setLeftInput(Value value);
    Value getRightInput();
    void setRightInput(Value value);

    default Type getType() {
        return getLeftInput().getType();
    }

    default int getValueDependencyCount() {
        return 2;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getLeftInput() : index == 1 ? getRightInput() : Util.throwIndexOutOfBounds(index);
    }
}
