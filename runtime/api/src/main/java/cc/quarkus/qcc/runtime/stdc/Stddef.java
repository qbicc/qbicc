package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 */
@include("<stddef.h>")
public final class Stddef {
    /**
     * An unsigned number of native units (defined as {@code char}s).
     */
    public static final class size_t extends word {}

    public static final class size_t_ptr extends ptr<size_t> {}
    public static final class const_size_t_ptr extends ptr<@c_const size_t> {}
    public static final class size_t_ptr_ptr extends ptr<size_t_ptr> {}
    public static final class const_size_t_ptr_ptr extends ptr<const_size_t_ptr> {}
    public static final class size_t_ptr_const_ptr extends ptr<@c_const size_t_ptr> {}
    public static final class const_size_t_ptr_const_ptr extends ptr<@c_const const_size_t_ptr> {}

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
            return word(Math.abs(longValue()));
        }
    }

    public static final class max_align_t extends word {
    }

    public static final class wchar_t extends word {
    }
}
