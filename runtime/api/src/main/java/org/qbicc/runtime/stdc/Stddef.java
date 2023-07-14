package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<stddef.h>")
public final class Stddef {
    /**
     * An unsigned number of native units (defined as {@code char}s).
     */
    public static final class size_t extends word {}

    /**
     * A signed number of native units (defined as {@code char}s).
     */
    public static final class ptrdiff_t extends word {
        /**
         * Get the absolute value of this pointer difference, as a {@link size_t}.
         *
         * @return the absolute value
         */
        public size_t abs() {
            return word(java.lang.Math.abs(longValue()));
        }
    }

    public static final class max_align_t extends word {
    }

    public static final class wchar_t extends word {
    }
}
