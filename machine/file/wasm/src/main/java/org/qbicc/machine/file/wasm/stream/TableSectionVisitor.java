package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public class TableSectionVisitor<E extends Exception> extends Visitor<E> {
    /**
     * Visit a table entry with no maximum.
     *
     * @param type the reference type (must not be {@code null})
     * @param min the minimum (unsigned) bound of the limit
     * @throws E if an error occurs
     */
    public void visitTable(RefType type, int min) throws E {
    }

    /**
     * Visit a table entry with an explicit maximum.
     *
     * @param type the reference type (must not be {@code null})
     * @param min the minimum (unsigned) bound of the limit
     * @param max the minimum (unsigned) bound of the limit (must be ≥ the minimum)
     * @throws E if an error occurs
     */
    public void visitTable(RefType type, int min, int max) throws E {
    }
}
