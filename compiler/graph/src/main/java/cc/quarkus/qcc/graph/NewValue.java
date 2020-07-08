package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation.
 */
public interface NewValue extends Value, MemoryState, GraphFactory.MemoryStateValue {
    ClassType getType();

    void setType(ClassType type);

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}
