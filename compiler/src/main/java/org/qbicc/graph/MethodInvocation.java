package org.qbicc.graph;

import org.qbicc.type.definition.element.MethodElement;

/**
 *
 */
public interface MethodInvocation extends Invocation {
    MethodElement getInvocationTarget();
}
