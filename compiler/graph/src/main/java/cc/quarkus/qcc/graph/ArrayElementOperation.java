package cc.quarkus.qcc.graph;

/**
 * An operation on an array element.
 */
public interface ArrayElementOperation extends MemoryState, InstanceOperation {
    Value getIndex();

    void setIndex(Value value);

    JavaAccessMode getMode();

    void setMode(JavaAccessMode mode);
}
