package org.qbicc.machine.llvm;

public enum ModuleFlagBehavior {
    Error(1),
    Warning(2),
    Require(3),
    Override(4),
    Append(5),
    AppendUnique(6),
    Max(7);

    public final int value;

    ModuleFlagBehavior(int value) {
        this.value = value;
    }
}
