package org.qbicc.machine.file.wasm;

/**
 *
 */
public enum TagAttribute {
    EXCEPTION,
    ;

    public static TagAttribute forId(int id) {
        return values()[id];
    }
}
