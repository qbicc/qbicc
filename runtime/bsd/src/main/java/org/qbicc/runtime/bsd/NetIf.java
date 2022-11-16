package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;

@include("<net/if.h>")
public class NetIf {
    public static native unsigned_int if_nametoindex(const_char_ptr ifname);
}
