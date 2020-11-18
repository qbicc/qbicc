package cc.quarkus.qcc.graph;

/**
 * A field write.
 */
public interface FieldWrite extends FieldOperation, WriteOperation {
    Value getWriteValue();

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }
}
