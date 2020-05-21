package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface ReturnValueInstruction extends ReturnInstruction {
    Value getReturnValue();
    void setReturnValue(Value value);
}
