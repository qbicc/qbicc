package org.qbicc.machine.file.wasm.model;

/**
 *
 */
public interface Limits {
    long minSize();

    long maxSize();

    boolean shared();
}
