package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 * An invoke instruction which returns a value.
 */
public interface InvocationValue extends Invocation, Value {
    MethodElement getInvocationTarget();

    default Type getType() {
        return getInvocationTarget().getReturnType();
    }
}
