package org.qbicc.machine.llvm;

/**
 *
 */
public enum Visibility {
    DEFAULT("default"),
    HIDDEN("hidden"),
    PROTECTED("protected"),
    ;

    private final String name;

    Visibility(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
