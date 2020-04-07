package cc.quarkus.c_native.stdc;

import static cc.quarkus.c_native.api.CNative.*;

/**
 *
 */
@include("<stddef.h>")
public final class Stddef {
    public static final class size_t extends word {
    }

    public static final class ptrdiff_t extends word {
    }

    public static final class max_align_t extends word {
    }

    public static final class wchar_t extends word {
    }
}
