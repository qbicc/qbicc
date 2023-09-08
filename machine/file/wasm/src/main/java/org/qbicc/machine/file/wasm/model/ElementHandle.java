package org.qbicc.machine.file.wasm.model;

import java.util.NoSuchElementException;

import io.smallrye.common.constraint.Assert;

/**
 * A handle for resolving elements lazily.
 */
public final class ElementHandle {
    private Element element;

    private ElementHandle() {}

    private ElementHandle(Element element) {
        this.element = element;
    }

    /**
     * Get the element.
     *
     * @return the element (not {@code null})
     * @throws NoSuchElementException if the handle is not yet initialized
     */
    public Element element() {
        Element element = this.element;
        if (element == null) {
            throw new NoSuchElementException("Element handle was not initialized");
        }
        return element;
    }

    /**
     * Initialize this handle with the given element.
     *
     * @param element the element to set this handle to (must not be {@code null})
     * @throws IllegalStateException if the handle is already initialized
     */
    public void initialize(Element element) {
        Assert.checkNotNullParam("element", element);
        if (this.element != null) {
            throw new IllegalStateException("Element handle was already initialized");
        }
        this.element = element;
    }

    /**
     * Construct a new initialized instance.
     *
     * @param element the element to initialize to (must not be {@code null})
     * @return the initialized element handle (not {@code null})
     */
    public static ElementHandle of(Element element) {
        Assert.checkNotNullParam("element", element);
        return new ElementHandle(element);
    }

    /**
     * Construct a new unresolved instance.
     *
     * @return the uninitialized element handle (not {@code null})
     */
    public static ElementHandle unresolved() {
        return new ElementHandle();
    }
}
