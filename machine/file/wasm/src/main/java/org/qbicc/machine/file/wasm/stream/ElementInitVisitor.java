package org.qbicc.machine.file.wasm.stream;

/**
 * A visitor for the initializers of an element.
 */
public class ElementInitVisitor<E extends Exception> extends Visitor<E> {
    /**
     * Visit the initializer for the next element.
     *
     * @return the initializer visitor, or {@code null} to skip processing of it
     */
    public InsnSeqVisitor<E> visitInit() {
        return null;
    }
}
