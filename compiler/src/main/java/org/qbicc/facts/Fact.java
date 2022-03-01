package org.qbicc.facts;

/**
 * A boolean fact which applies to a specific element type.
 */
public interface Fact<E> {
    Class<E> getElementType();
}
