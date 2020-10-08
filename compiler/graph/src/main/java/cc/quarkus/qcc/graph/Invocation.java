package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

/**
 * A method invocation.
 */
public interface Invocation extends Node {
    ParameterizedExecutableElement getInvocationTarget();

    @Deprecated
    ClassType getMethodOwner();
    @Deprecated
    void setMethodOwner(ClassType owner);

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
