package cc.quarkus.c_native.linux;

import static cc.quarkus.c_native.api.CNative.*;

/**
 *
 */
@include("<unistd.h>")
@define(value = "_GNU_SOURCE")
public final class Unistd {
    public static native c_int pipe2(c_int @array_size(2) [] fds, c_int flags);

    public static native c_int dup3(c_int fd1, c_int fd2, c_int flags);

}
