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
}
