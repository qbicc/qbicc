package org.qbicc.runtime.stdc;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<stdint.h>")
public final class Stdint {
    public static final class int8_t extends word {}
    public static final class int8_t_ptr extends ptr<int8_t> {}
    public static final class const_int8_t_ptr extends ptr<@c_const int8_t> {}
    public static final class int8_t_ptr_ptr extends ptr<int8_t_ptr> {}
    public static final class const_int8_t_ptr_ptr extends ptr<const_int8_t_ptr> {}
    public static final class int8_t_ptr_const_ptr extends ptr<@c_const int8_t_ptr> {}
    public static final class const_int8_t_ptr_const_ptr extends ptr<@c_const const_int8_t_ptr> {}

    public static final class int16_t extends word {}
    public static final class int16_t_ptr extends ptr<int16_t> {}
    public static final class const_int16_t_ptr extends ptr<@c_const int16_t> {}
    public static final class int16_t_ptr_ptr extends ptr<int16_t_ptr> {}
    public static final class const_int16_t_ptr_ptr extends ptr<const_int16_t_ptr> {}
    public static final class int16_t_ptr_const_ptr extends ptr<@c_const int16_t_ptr> {}
    public static final class const_int16_t_ptr_const_ptr extends ptr<@c_const const_int16_t_ptr> {}

    public static final class int32_t extends word {}
    public static final class int32_t_ptr extends ptr<int32_t> {}
    public static final class const_int32_t_ptr extends ptr<@c_const int32_t> {}
    public static final class int32_t_ptr_ptr extends ptr<int32_t_ptr> {}
    public static final class const_int32_t_ptr_ptr extends ptr<const_int32_t_ptr> {}
    public static final class int32_t_ptr_const_ptr extends ptr<@c_const int32_t_ptr> {}
    public static final class const_int32_t_ptr_const_ptr extends ptr<@c_const const_int32_t_ptr> {}

    public static final class int64_t extends word {}
    public static final class int64_t_ptr extends ptr<int64_t> {}
    public static final class const_int64_t_ptr extends ptr<@c_const int64_t> {}
    public static final class int64_t_ptr_ptr extends ptr<int64_t_ptr> {}
    public static final class const_int64_t_ptr_ptr extends ptr<const_int64_t_ptr> {}
    public static final class int64_t_ptr_const_ptr extends ptr<@c_const int64_t_ptr> {}
    public static final class const_int64_t_ptr_const_ptr extends ptr<@c_const const_int64_t_ptr> {}

    public static final class uint8_t extends word {}
    public static final class uint8_t_ptr extends ptr<uint8_t> {}
    public static final class const_uint8_t_ptr extends ptr<@c_const uint8_t> {}
    public static final class uint8_t_ptr_ptr extends ptr<uint8_t_ptr> {}
    public static final class const_uint8_t_ptr_ptr extends ptr<const_uint8_t_ptr> {}
    public static final class uint8_t_ptr_const_ptr extends ptr<@c_const uint8_t_ptr> {}
    public static final class const_uint8_t_ptr_const_ptr extends ptr<@c_const const_uint8_t_ptr> {}

    public static final class uint16_t extends word {}
    public static final class uint16_t_ptr extends ptr<uint16_t> {}
    public static final class const_uint16_t_ptr extends ptr<@c_const uint16_t> {}
    public static final class uint16_t_ptr_ptr extends ptr<uint16_t_ptr> {}
    public static final class const_uint16_t_ptr_ptr extends ptr<const_uint16_t_ptr> {}
    public static final class uint16_t_ptr_const_ptr extends ptr<@c_const uint16_t_ptr> {}
    public static final class const_uint16_t_ptr_const_ptr extends ptr<@c_const const_uint16_t_ptr> {}

    public static final class uint32_t extends word {}
    public static final class uint32_t_ptr extends ptr<uint32_t> {}
    public static final class const_uint32_t_ptr extends ptr<@c_const uint32_t> {}
    public static final class uint32_t_ptr_ptr extends ptr<uint32_t_ptr> {}
    public static final class const_uint32_t_ptr_ptr extends ptr<const_uint32_t_ptr> {}
    public static final class uint32_t_ptr_const_ptr extends ptr<@c_const uint32_t_ptr> {}
    public static final class const_uint32_t_ptr_const_ptr extends ptr<@c_const const_uint32_t_ptr> {}

    public static final class uint64_t extends word {}
    public static final class uint64_t_ptr extends ptr<uint64_t> {}
    public static final class const_uint64_t_ptr extends ptr<@c_const uint64_t> {}
    public static final class uint64_t_ptr_ptr extends ptr<uint64_t_ptr> {}
    public static final class const_uint64_t_ptr_ptr extends ptr<const_uint64_t_ptr> {}
    public static final class uint64_t_ptr_const_ptr extends ptr<@c_const uint64_t_ptr> {}
    public static final class const_uint64_t_ptr_const_ptr extends ptr<@c_const const_uint64_t_ptr> {}

    public static final class intptr_t extends word {}
    public static final class uintptr_t extends word {}
}
