package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stdc.Stdint.*;

@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<arpa/inet.h>")
public class ArpaInet {
    public static native uint64_t htonll(uint64_t hostlonglong);
    public static native uint32_t htonl(uint32_t hostlong);
    public static native uint16_t hton2(uint16_t hostshort);

    public static native uint64_t ntohll(uint64_t netlonglong);
    public static native uint32_t ntohl(uint32_t netlong);
    public static native uint16_t ntohs(uint16_t netshort);
}
