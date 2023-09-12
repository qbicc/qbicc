package org.qbicc.machine.file.wasm.stream;

import org.qbicc.machine.file.wasm.Mutability;
import org.qbicc.machine.file.wasm.ValType;

/**
 *
 */
public class GlobalSectionVisitor<E extends Exception> extends Visitor<E> {
    public InsnSeqVisitor<E> visitGlobal(ValType type, Mutability mut) throws E {
        return null;
    }
}
