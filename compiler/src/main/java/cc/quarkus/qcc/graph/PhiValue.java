package cc.quarkus.qcc.graph;

/**
 *
 */
public interface PhiValue extends ProgramNode, Value {
    Value getValueForBlock(BasicBlock input);
    void setValueForBlock(BasicBlock input, Value value);

    Type getType();
    void setType(Type type);
}
