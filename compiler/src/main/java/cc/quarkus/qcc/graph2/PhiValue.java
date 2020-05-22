package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface PhiValue extends Value {
    Value getValueForBlock(BasicBlock input);
    void setValueForBlock(BasicBlock input, Value value);
}
