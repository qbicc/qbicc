package cc.quarkus.c_native.stdc;

import static cc.quarkus.c_native.api.CNative.*;
import static cc.quarkus.c_native.stdc.Stddef.*;

/**
 *
 */
@include("<string.h>")
public class String {
    public static native size_t strlen(ptr<@c_const c_char> s);

    public static native ptr<c_char> strerror(c_int errNum);
}
