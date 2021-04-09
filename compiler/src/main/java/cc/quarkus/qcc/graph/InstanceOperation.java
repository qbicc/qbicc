package org.qbicc.graph;

/**
 * An operation which dereferences an object instance.  After the operation, the value is guaranteed to be non-{@code null}.
 */
public interface InstanceOperation extends Node {
    Value getInstance();

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : Util.throwIndexOutOfBounds(index);
    }
}
