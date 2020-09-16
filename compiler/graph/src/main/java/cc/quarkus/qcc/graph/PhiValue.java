package cc.quarkus.qcc.graph;

/**
 *
 */
public interface PhiValue extends PinnedNode, Value {
    Value getValueForBlock(BasicBlock input);
    void setValueForBlock(BasicBlock input, Value value);
    void setValueForBlock(NodeHandle input, Value value);

    Type getType();
    void setType(Type type);
}
