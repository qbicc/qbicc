package cc.quarkus.qcc.graph;

/**
 * An operation on a field.
 */
public interface FieldOperation extends Node {
    ClassType getFieldOwner();

    void setFieldOwner(ClassType fieldOwner);

    String getFieldName();

    void setFieldName(String fieldName);

    JavaAccessMode getMode();

    void setMode(JavaAccessMode mode);
}
