package cc.quarkus.qcc.graph;

/**
 *
 */
public abstract class FieldOperationImpl extends MemoryStateImpl implements FieldOperation {
    ClassType owner;
    String fieldName;
    FieldOperation.Mode mode = FieldOperation.Mode.DETECT;

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

    public FieldOperation.Mode getMode() {
        return mode;
    }

    public void setMode(final FieldOperation.Mode mode) {
        this.mode = mode;
    }
}
