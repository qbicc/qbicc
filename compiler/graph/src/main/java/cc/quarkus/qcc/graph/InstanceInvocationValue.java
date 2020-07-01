package cc.quarkus.qcc.graph;

/**
 * An invocation on an object instance which returns a value.
 */
public interface InstanceInvocationValue extends InstanceOperation, InvocationValue {
    default int getValueDependencyCount() {
        return InvocationValue.super.getValueDependencyCount();
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return InvocationValue.super.getValueDependency(index);
    }
}
