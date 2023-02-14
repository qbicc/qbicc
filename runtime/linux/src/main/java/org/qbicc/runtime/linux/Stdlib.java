package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@include("<stdlib.h>")
@define(value = "_DEFAULT_SOURCE")
public final class Stdlib {

    public static native c_int getloadavg(ptr<_Float64> loadavg, c_int nelem);
}
