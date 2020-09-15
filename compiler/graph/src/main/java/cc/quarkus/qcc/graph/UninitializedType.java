package cc.quarkus.qcc.graph;

/**
 * A type representing an uninitialized object instance of some sort.
 */
public interface UninitializedType extends Type {
    /**
     * Get the type of the object that is to be initialized.
     *
     * @return the class type (not {@code null})
     */
    ClassType getClassType();
}
