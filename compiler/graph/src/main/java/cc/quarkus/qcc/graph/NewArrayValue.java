package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation for arrays.
 */
public interface NewArrayValue extends Value, MemoryState, GraphFactory.MemoryStateValue {
    ArrayClassType getType();

    void setType(ArrayClassType type);

    Value getSize();

    void setSize(Value size);

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getSize() : Util.throwIndexOutOfBounds(index);
    }
}
