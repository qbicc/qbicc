package org.qbicc.pointer;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 * Interface which marks a direct pointer to some executable element.
 */
public interface ExecutableElementPointer {
    ExecutableElement getExecutableElement();
}
