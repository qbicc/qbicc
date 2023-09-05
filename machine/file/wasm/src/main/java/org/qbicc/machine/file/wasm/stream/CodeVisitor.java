package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public class CodeVisitor<E extends Exception> extends Visitor<E> {
    public void visitLocal(int count, ValType type) throws E {
    }

    public InsnSeqVisitor<E> visitBody() throws E {
        return null;
    }
}
