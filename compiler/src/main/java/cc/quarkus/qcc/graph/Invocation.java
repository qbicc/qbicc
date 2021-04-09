package org.qbicc.graph;

import org.qbicc.type.definition.element.InvokableElement;

/**
 * An invocation.
 */
public interface Invocation extends Node {
    InvokableElement getInvocationTarget();
    int getArgumentCount();
    Value getArgument(int index);

    default int getValueDependencyCount() {
        return getArgumentCount();
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return getArgument(index);
    }
}
