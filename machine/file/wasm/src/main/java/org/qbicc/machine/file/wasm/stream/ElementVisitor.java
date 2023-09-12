package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.RefType;

/**
 *
 */
public class ElementVisitor<E extends Exception> extends Visitor<E> {
    public void visitType(RefType refType) throws E {
    }

    /**
     * Visit the initializer as a vector of function indices.
     *
     * @param funcIdx the function indices (must not be {@code null})
     * @throws E if the visitor fails
     */
    public void visitInit(int... funcIdx) throws E {
    }

    /**
     * Visit one initializer as a constant expression.
     *
     * @return the element init visitor, or {@code null} to skip the initializer
     * @throws E if the visitor fails
     */
    public ElementInitVisitor<E> visitInit() throws E {
        return null;
    }
}
