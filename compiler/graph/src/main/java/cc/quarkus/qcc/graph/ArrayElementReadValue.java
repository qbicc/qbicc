package cc.quarkus.qcc.graph;

/**
 * A read of an array element.
 */
public interface ArrayElementReadValue extends Value, ArrayElementOperation, GraphFactory.MemoryStateValue {
    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}
