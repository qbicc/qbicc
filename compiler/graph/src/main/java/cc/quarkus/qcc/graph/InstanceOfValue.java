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

    static InstanceOfValue create(Value instance, ClassType type) {
        InstanceOfValueImpl instanceOfValue = new InstanceOfValueImpl();
        instanceOfValue.setInstance(instance);
        instanceOfValue.setInstanceType(type);
        return instanceOfValue;
    }
}
