package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface BinaryValue extends Value, ProgramNode {
    Value getLeftInput();
    void setLeftInput(Value value);
    Value getRightInput();
    void setRightInput(Value value);
}
