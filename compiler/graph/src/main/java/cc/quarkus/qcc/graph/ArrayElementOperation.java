package cc.quarkus.qcc.graph;

/**
 * An operation on an array element.
 */
public interface ArrayElementOperation extends MemoryState, InstanceOperation {
    Value getIndex();

    void setIndex(Value value);

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
