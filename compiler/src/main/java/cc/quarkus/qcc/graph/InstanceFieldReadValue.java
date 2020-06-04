package cc.quarkus.qcc.graph;

/**
 * A read of an instance field.
 */
public interface InstanceFieldReadValue extends FieldReadValue {
    Value getInstance();
    void setInstance(Value value);
}
