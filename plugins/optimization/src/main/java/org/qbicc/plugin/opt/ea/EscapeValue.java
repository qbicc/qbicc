package org.qbicc.plugin.opt.ea;

import java.util.Objects;

enum EscapeValue {
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

    static EscapeValue of(EscapeValue escapeValue) {
        return Objects.isNull(escapeValue) ? EscapeValue.UNKNOWN : escapeValue;
    }
}
