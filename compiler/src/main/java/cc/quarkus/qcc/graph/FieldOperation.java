package cc.quarkus.qcc.graph;

/**
 * An operation on a field.
 */
public interface FieldOperation extends MemoryState {
    Object getFieldDescriptor();

    void setFieldDescriptor(Object fieldDescriptor);

    Mode getMode();

    void setMode(Mode mode);

    enum Mode {
        /**
         * Detect access mode from field declaration.
         */
        DETECT,
        /**
         * Plain (opaque) access.
         */
        PLAIN,
        /**
         * Ordered access.
         */
        ORDERED,
        /**
         * Volatile access.
         */
        VOLATILE,
        ;
    }
}
