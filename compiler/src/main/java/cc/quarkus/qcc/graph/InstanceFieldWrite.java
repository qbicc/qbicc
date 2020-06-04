package cc.quarkus.qcc.graph;

/**
 * A write of an instance field.
 */
public interface InstanceFieldWrite extends FieldWrite {
    Value getInstance();
    void setInstance(Value value);
}
