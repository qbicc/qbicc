package cc.quarkus.qcc.graph;

/**
 * A class (or interface) type.  If a class (or interface) with a given name exists in more than one class loader,
 * each one will have a corresponding distinct instance of this type.
 */
public interface ClassType extends Type {
    // todo: compact package and class names
    String getClassName();
}
