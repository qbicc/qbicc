package org.qbicc.driver;

import java.util.function.Consumer;

import org.qbicc.type.definition.element.ExecutableElement;

/**
 * An element consumer which creates the element body, if any.
 */
public final class ElementBodyCreator implements Consumer<ExecutableElement> {
    /**
     * Construct a new instance.
     */
    public ElementBodyCreator() {
    }

    @Override
    public void accept(ExecutableElement executableElement) {
        executableElement.tryCreateMethodBody();
    }
}
