package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface IfValue extends ProgramNode, Value {
    Value getCond();
    void setCond(Value cond);
    Value getTrueValue();
    void setTrueValue(Value value);
    Value getFalseValue();
    void setFalseValue(Value value);
}
