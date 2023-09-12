package org.qbicc.machine.file.wasm;

/**
 *
 */
public enum Mutability {
    const_(0),
    var_(1),
    ;
    private final int val;

    Mutability(int val) {
        this.val = val;
    }

    public int byteValue() {
        return val;
    }
}
