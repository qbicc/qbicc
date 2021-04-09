package org.qbicc.machine.llvm;

/**
 *
 */
public enum RuntimePreemption {
    PREEMPTABLE("dso_preemptable"),
    LOCAL("dso_local"),
    ;

    private final String name;

    RuntimePreemption(final String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }
}
