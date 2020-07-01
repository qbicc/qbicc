package cc.quarkus.qcc.graph;

/**
 * A field write.
 */
public interface FieldWrite extends FieldOperation {
    Value getWriteValue();
    void setWriteValue(Value value);

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }
}
