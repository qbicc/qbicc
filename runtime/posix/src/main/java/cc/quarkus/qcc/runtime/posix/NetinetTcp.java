package cc.quarkus.qcc.runtime.posix;

import static cc.quarkus.qcc.runtime.CNative.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<netinet/tcp.h>")
public class NetinetTcp {
    public static final c_int TCP_NODELAY = constant();
}
