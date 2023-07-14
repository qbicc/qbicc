package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<stdint.h>")
public final class Stdint {
    public static final class int8_t extends word {}
    public static final class int16_t extends word {}
    public static final class int32_t extends word {}
    public static final class int64_t extends word {}

    public static final class uint8_t extends word {}
    public static final class uint16_t extends word {}
    public static final class uint32_t extends word {}
    public static final class uint64_t extends word {}

    public static final class intptr_t extends word {}
    public static final class uintptr_t extends word {}
}
