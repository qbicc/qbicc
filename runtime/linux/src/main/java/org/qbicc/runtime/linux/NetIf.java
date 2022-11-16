package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

@include("<net/if.h>")
public class NetIf {
    public static final unsigned_long SIOCGIFINDEX = constant();
}
