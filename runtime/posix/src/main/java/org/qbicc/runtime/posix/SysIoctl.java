package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;

@SuppressWarnings("SpellCheckingInspection")
@include("<sys/ioctl.h>")
public class SysIoctl {

    public static native c_int ioctl(c_int fd, unsigned_long request, object... more);

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
