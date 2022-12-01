package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysSocket.*;

@include("<ifaddrs.h>")
public class Ifaddrs {
    public static class struct_ifaddrs extends object {
        public ptr<struct_ifaddrs> ifa_next;
        public char_ptr ifa_name;
        public unsigned_int ifa_flags;
        public struct_sockaddr_ptr ifa_addr;
        public struct_sockaddr_ptr ifa_netmask;
        public struct_sockaddr_ptr ifa_dstaddr;
        public void_ptr ifa_data;
    }

    public static class struct_ifaddrs_ptr extends ptr<struct_ifaddrs> {}
    public static class struct_ifaddrs_ptr_ptr extends ptr<struct_ifaddrs_ptr> {}

    public static native c_int getifaddrs(struct_ifaddrs_ptr_ptr x);
    public static native void freeifaddrs(struct_ifaddrs_ptr x);
}
