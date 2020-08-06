package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.descriptor.MethodIdentifier;

/**
 * A method invocation.
 */
public interface Invocation extends Node {
    ClassType getMethodOwner();
    void setMethodOwner(ClassType owner);

    MethodIdentifier getInvocationTarget();

    /**
     * Set the invocation target, which in turn also sets the argument count.
     *
     * @param descriptor the method descriptor of the invocation target
     */
    void setInvocationTarget(MethodIdentifier descriptor);
    int getArgumentCount();
    void setArgumentCount(int newCount);
    Value getArgument(int index);
    void setArgument(int index, Value argument);

    default int getValueDependencyCount() {
        return getArgumentCount();
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return getArgument(index);
    }
}
