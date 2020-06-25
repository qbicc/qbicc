package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation.
 */
public interface NewValue extends Value, MemoryState {
    ClassType getType();

    void setType(ClassType type);

    static NewValue create(ClassType classType) {
        NewValueImpl value = new NewValueImpl();
        value.setType(classType);
        return value;
    }
}
