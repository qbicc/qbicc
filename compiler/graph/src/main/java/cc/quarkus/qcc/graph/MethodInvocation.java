package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
public interface MethodInvocation extends Invocation {
    MethodElement getInvocationTarget();
}
