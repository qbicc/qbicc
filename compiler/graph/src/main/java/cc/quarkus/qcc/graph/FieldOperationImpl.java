package cc.quarkus.qcc.graph;

/**
 *
 */
abstract class FieldOperationImpl extends MemoryStateImpl implements FieldOperation {
    ClassType owner;
    String fieldName;
    JavaAccessMode mode = JavaAccessMode.DETECT;

    public ClassType getFieldOwner() {
        return owner;
    }

    public void setFieldOwner(final ClassType fieldOwner) {
        this.owner = fieldOwner;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(final String fieldName) {
        this.fieldName = fieldName;
    }

    public JavaAccessMode getMode() {
        return mode;
    }

    public void setMode(final JavaAccessMode mode) {
        this.mode = mode;
    }
}
