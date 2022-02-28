package org.qbicc.type.definition;

import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.ParameterElement;

/**
 * A resolver for a method parameter.
 */
public interface ParameterResolver {
    /**
     * Resolve the parameter element from the given builder.
     *
     * @param element the enclosing invokable element (must not be {@code null})
     * @param index the index of the parameter, starting at 0
     * @param builder the parameter element builder
     * @return the resolved parameter element (must not be {@code null})
     */
    ParameterElement resolveParameter(InvokableElement element, int index, ParameterElement.Builder builder);
}
