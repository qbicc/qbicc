package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

@include("<net/if.h>")
public class NetIf {
    // TODO: Remove this definition once we do a classlib update.
    //       This definition belongs in SysIoctl
    public static final unsigned_long SIOCGIFINDEX = constant();
}
