package org.qbicc.machine.file.wasm.stream;

/**
 *
 */
public abstract class Visitor<E extends Exception> {
    public void visitEnd() throws E {
    }
}
