package org.qbicc.runtime.linux;

import static org.qbicc.runtime.CNative.*;

/**
 *
 */
@SuppressWarnings("SpellCheckingInspection")
@include("<unistd.h>")
@define(value = "_GNU_SOURCE")
public final class Unistd {
    public static native c_int pipe2(c_int @array_size(2) [] fds, c_int flags);

    public static native c_int dup3(c_int fd1, c_int fd2, c_int flags);

    public static final c_int _SC_PHYS_PAGES = constant();
    public static final c_int _SC_AVPHYS_PAGES = constant();
    public static final c_int _SC_NPROCESSORS_CONF = constant();
    public static final c_int _SC_NPROCESSORS_ONLN = constant();
}
