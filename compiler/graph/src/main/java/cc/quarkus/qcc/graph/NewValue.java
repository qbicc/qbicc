package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation.
 */
public interface NewValue extends Value, MemoryState, GraphFactory.MemoryStateValue {
    ClassType getType();

    void setType(ClassType type);

    static NewValue create(ClassType classType) {
        NewValueImpl value = new NewValueImpl();
        value.setType(classType);
        return value;
    }

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}
