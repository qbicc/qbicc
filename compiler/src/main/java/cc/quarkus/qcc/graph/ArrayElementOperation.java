package cc.quarkus.qcc.graph;

/**
 * An operation on an array element.
 */
public interface ArrayElementOperation extends InstanceOperation {
    Value getIndex();

    JavaAccessMode getMode();
}
