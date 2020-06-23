package cc.quarkus.qcc.graph;

/**
 * An operation which dereferences an object instance.  After the operation, the value is guaranteed to be non-{@code null}.
 */
public interface InstanceOperation {
    Value getInstance();

    void setInstance(Value value);
}
