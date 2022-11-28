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

    public static final unsigned_long SIOCSHIWAT = constant();
    public static final unsigned_long SIOCGHIWAT = constant();
    public static final unsigned_long SIOCSLOWAT = constant();
    public static final unsigned_long SIOCGLOWAT = constant();
    public static final unsigned_long SIOCATMARK = constant();
    public static final unsigned_long SIOCSPGRP = constant();
    public static final unsigned_long SIOCGPGRP = constant();

    public static final unsigned_long SIOCSIFADDR = constant();
    public static final unsigned_long SIOCSIFDSTADDR = constant();
    public static final unsigned_long SIOCSIFFLAGS = constant();
    public static final unsigned_long SIOCGIFFLAGS = constant();
    public static final unsigned_long SIOCSIFBRDADDR = constant();
    public static final unsigned_long SIOCSIFNETMASK = constant();
    public static final unsigned_long SIOCGIFMETRIC = constant();
    public static final unsigned_long SIOCSIFMETRIC = constant();
    public static final unsigned_long SIOCDIFADDR = constant();
    public static final unsigned_long SIOCAIFADDR = constant();

    public static final unsigned_long SIOCGIFADDR = constant();
    public static final unsigned_long SIOCGIFDSTADDR = constant();
    public static final unsigned_long SIOCGIFBRDADDR = constant();
    public static final unsigned_long SIOCGIFCONF = constant();
    public static final unsigned_long SIOCGIFNETMASK = constant();
    public static final unsigned_long SIOCAUTOADDR = constant();
    public static final unsigned_long SIOCAUTONETMASK = constant();
    public static final unsigned_long SIOCARPIPLL = constant();

    public static final unsigned_long SIOCADDMULTI = constant();
    public static final unsigned_long SIOCDELMULTI = constant();
    public static final unsigned_long SIOCGIFMTU = constant();
    public static final unsigned_long SIOCSIFMTU = constant();
    public static final unsigned_long SIOCGIFPHYS = constant();
    public static final unsigned_long SIOCSIFPHYS = constant();
    public static final unsigned_long SIOCSIFMEDIA = constant();
}