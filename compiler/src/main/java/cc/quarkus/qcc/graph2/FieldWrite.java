package cc.quarkus.qcc.graph2;

/**
 * A field write.
 */
public interface FieldWrite extends MemoryState, FieldOperation {
    Value getWriteValue();
    void setWriteValue(Value value);
}
