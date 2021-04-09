package org.qbicc.type.definition;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A factory for producing the method body for a given executable element.
 */
public interface MethodBodyFactory {
    MethodBody createMethodBody(int index, ExecutableElement element);
}
