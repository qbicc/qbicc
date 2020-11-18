package cc.quarkus.qcc.runtime.stdc;

import static cc.quarkus.qcc.runtime.api.CNative.*;
import static cc.quarkus.qcc.runtime.stdc.Stddef.*;

/**
 *
 */
@include("<string.h>")
public class String {
    public static native size_t strlen(ptr<@c_const c_char> s);

    public static native ptr<c_char> strerror(c_int errNum);
}
