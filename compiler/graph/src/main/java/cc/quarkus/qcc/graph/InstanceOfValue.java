package cc.quarkus.qcc.graph;

/**
 *
 */
public interface InstanceOfValue extends InstanceOperation, Value {
    default Type getType() {
        return Type.BOOL;
    }

    ClassType getInstanceType();

    void setInstanceType(ClassType classType);
}
