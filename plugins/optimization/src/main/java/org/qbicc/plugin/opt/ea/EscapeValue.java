package org.qbicc.plugin.opt.ea;

import java.util.Objects;

enum EscapeValue {
    // Keep the order of elements as they are.
    // The ordering among the lattice elements is: GlobalEscape < ArgEscape < NoEscape < Unknown
    GLOBAL_ESCAPE, ARG_ESCAPE, NO_ESCAPE, UNKNOWN;

    boolean isArgEscape() {
        return this == ARG_ESCAPE;
    }

    boolean isGlobalEscape() {
        return this == GLOBAL_ESCAPE;
    }

    boolean notGlobalEscape() {
        return !isGlobalEscape();
    }

    boolean isNoEscape() {
        return this == NO_ESCAPE;
    }

    private boolean isMoreThan(EscapeValue other) {
        return this.compareTo(other) > 0;
    }

    boolean isMoreThanArgEscape() {
        return isMoreThan(ARG_ESCAPE);
    }

    static EscapeValue of(EscapeValue escapeValue) {
        return Objects.isNull(escapeValue) ? EscapeValue.UNKNOWN : escapeValue;
    }

    static EscapeValue merge(EscapeValue a, EscapeValue b) {
        if (b.isGlobalEscape())
            return GLOBAL_ESCAPE;

        if (a.isNoEscape())
            return b;

        return a;
    }
}
