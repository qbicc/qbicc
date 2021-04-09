package org.qbicc.machine.llvm;

/**
 *
 */
public enum Linkage {
    PRIVATE("private"),
    INTERNAL("internal"),
    AVAILABLE_EXTERNALLY("available_externally"),
    LINK_ONCE("linkonce"),
    WEAK("weak"),
    COMMON("common"),
    APPENDING("appending"),
    EXTERN_WEAK("extern_weak"),
    LINK_ONCE_ODR("linkonce_odr"),
    WEAK_ODR("weak_odr"),
    EXTERNAL("external"),
    ;

    private final String name;

    Linkage(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
