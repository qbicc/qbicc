package cc.quarkus.qcc.graph;

/**
 *
 */
public interface ArrayElementWrite extends ArrayElementOperation {
    Value getWriteValue();
    void setWriteValue(Value value);
}
