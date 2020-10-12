package cc.quarkus.qcc.graph;

/**
 *
 */
public interface WriteOperation extends Node {
    Value getWriteValue();

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }
}
