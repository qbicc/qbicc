package org.qbicc.runtime.posix;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;

@define(value = "_POSIX_C_SOURCE", as = "200809L")
@define(value = "_DARWIN_C_SOURCE", when = Build.Target.IsApple.class)
@include("<net/if.h>")
public class NetIf {

    public static final c_int IF_NAMESIZE = constant();

    public static final class struct_ifreq extends object {
        public c_char[] ifr_name;
        public void_ptr ifr_ifru; // This is actually a horrific 16-way union.
    }
    public static final class struct_ifreq_ptr extends ptr<struct_ifreq> {}

    public static final class struct_ifconf extends object {
        public c_int ifc_len;
        public void_ptr ifc_ifcu; // This is a 2-way union
    }
    public static final class struct_ifconf_ptr extends ptr<struct_ifconf> {}

    public static final c_short IFF_UP = constant();
    public static final c_short IFF_BROADCAST = constant();
    public static final c_short IFF_DEBUG = constant();
    public static final c_short IFF_LOOPBACK = constant();
    public static final c_short IFF_POINTOPOINT = constant();
    public static final c_short IFF_NOTRAILERS = constant();
    public static final c_short IFF_RUNNING = constant();
    public static final c_short IFF_NOARP = constant();
    public static final c_short IFF_PROMISC = constant();
    public static final c_short IFF_ALLMULTI = constant();
    public static final c_short IFF_OACTIVE = constant();
    public static final c_short IFF_SIMPLEX = constant();
    public static final c_short IFF_LINK0 = constant();
    public static final c_short IFF_LINK1 = constant();
    public static final c_short IFF_LINK2 = constant();
    public static final c_short IFF_ALTPHYS = constant();
    public static final c_short IFF_MULTICAST = constant();
}