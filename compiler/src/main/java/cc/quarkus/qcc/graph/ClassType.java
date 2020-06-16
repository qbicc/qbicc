package cc.quarkus.qcc.graph;

/**
 * A class (or interface) type.  If a class (or interface) with a given name exists in more than one class loader,
 * each one will have a corresponding distinct instance of this type.
 */
public interface ClassType extends Type {
    // todo: compact package and class names
    String getClassName();

    ClassType getSuperClass();

    int getInterfaceCount();

    InterfaceType getInterface(int index) throws IndexOutOfBoundsException;

    boolean isAssignableFrom(ClassType other);

    /**
     * Get the type that is an array class of this type.
     *
     * @return the type that is an array of this type
     */
    ArrayClassType getArrayType();
}
