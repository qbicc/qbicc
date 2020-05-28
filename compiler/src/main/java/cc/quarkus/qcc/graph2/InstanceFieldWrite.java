package cc.quarkus.qcc.graph2;

/**
 * A write of an instance field.
 */
public interface InstanceFieldWrite extends FieldWrite {
    Value getInstance();
    void setInstance(Value value);
}
