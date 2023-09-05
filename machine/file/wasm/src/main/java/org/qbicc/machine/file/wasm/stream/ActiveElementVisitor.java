package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public class ActiveElementVisitor<E extends Exception> extends ElementVisitor<E> {
    public void visitTableIndex(int index) throws E {
    }

    public InsnSeqVisitor<E> visitOffset() throws E {
        return null;
    }
}
