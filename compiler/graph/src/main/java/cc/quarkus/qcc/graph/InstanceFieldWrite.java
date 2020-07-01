package cc.quarkus.qcc.graph;

/**
 * A write of an instance field.
 */
public interface InstanceFieldWrite extends FieldWrite, InstanceOperation {
    default int getValueDependencyCount() {
        return 2;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : index == 1 ? getWriteValue() : Util.throwIndexOutOfBounds(index);
    }
}
