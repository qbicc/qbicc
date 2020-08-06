package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation.
 */
public interface NewValue extends Value {
    ClassType getType();

    void setType(ClassType type);
}
