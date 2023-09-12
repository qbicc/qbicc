package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public class ActiveDataVisitor<E extends Exception> extends DataVisitor<E> {
    public InsnSeqVisitor<E> visitOffset() throws E {
        return null;
    }
}
