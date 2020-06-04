package cc.quarkus.qcc.graph;

/**
 *
 */
public abstract class FieldOperationImpl extends MemoryStateImpl implements FieldOperation {
    Object fieldDescriptor;
    FieldOperation.Mode mode = FieldOperation.Mode.DETECT;

    public Object getFieldDescriptor() {
        return fieldDescriptor;
    }

    public void setFieldDescriptor(final Object fieldDescriptor) {
        this.fieldDescriptor = fieldDescriptor;
    }

    public FieldOperation.Mode getMode() {
        return mode;
    }

    public void setMode(final FieldOperation.Mode mode) {
        this.mode = mode;
    }
}
