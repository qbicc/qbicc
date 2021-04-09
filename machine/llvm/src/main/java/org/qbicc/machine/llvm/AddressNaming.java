package org.qbicc.machine.llvm;

/**
 *
 */
public enum AddressNaming {
    NAMED("named"),
    UNNAMED("unnamed_addr"),
    LOCAL_UNNAMED("local_unnamed_addr"),
    ;

    private final String name;

    AddressNaming(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
