package cc.quarkus.qcc.graph;

/**
 * An invocation on an object instance.
 */
public interface InstanceInvocation extends InstanceOperation, Invocation {
    default int getValueDependencyCount() {
        return Invocation.super.getValueDependencyCount() + 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInstance() : Invocation.super.getValueDependency(index - 1);
    }
}
