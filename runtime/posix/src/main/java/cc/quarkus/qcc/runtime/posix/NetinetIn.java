package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdint.*;

/**
 *
 */
@define(value = "_POSIX_C_SOURCE", as = "200809L")
@include("<netinet/in.h>")
public final class NetinetIn {
    public static final class in_port_t extends word {}
    public static final class in_addr_t extends word {}

    public static final class struct_in_addr extends object {
        public in_addr_t s_addr;
    }

    public static final class struct_sockaddr_in extends object {
        public sa_family_t sin_family;
        public in_port_t sin_port;
        public struct_in_addr sin_addr;
    }

    public static final class struct_in6_addr extends object {
        public uint8_t @array_size(16)[] s6_addr;
    }

    public static final class struct_sockaddr_in6 extends object {
        public sa_family_t sin6_family;
        public in_port_t sin6_port;
        public uint32_t sin6_flowinfo;
        public struct_in6_addr sin6_addr;
        public uint32_t sin6_scope_id;
    }

    @extern
    public static final @c_const struct_in6_addr in6addr_any = constant(); //todo: revisit overloading of constant() here
    @extern
    public static final @c_const struct_in6_addr in6addr_loopback = constant(); //todo: revisit overloading of constant() here

    public static final class struct_ipv6_mreq extends object {
        public struct_in6_addr ipv6mr_multiaddr;
        public unsigned_int ipv6mr_interface;
    }

    public static final c_int IPPROTO_IP = constant();
    public static final c_int IPPROTO_IPV6 = constant();
    public static final c_int IPPROTO_ICMP = constant();
    public static final c_int IPPROTO_RAW = constant();
    public static final c_int IPPROTO_TCP = constant();
    public static final c_int IPPROTO_UDP = constant();

    public static final in_addr_t INADDR_ANY = constant();
    public static final in_addr_t INADDR_BROADCAST = constant();

    public static final size_t INET_ADDRSTRLEN = constant();
    public static final size_t INET6_ADDRSTRLEN = constant();

    public static final c_int IPV6_JOIN_GROUP = constant();
    public static final c_int IPV6_LEAVE_GROUP = constant();
    public static final c_int IPV6_MULTICAST_HOPS = constant();
    public static final c_int IPV6_MULTICAST_IF = constant();
    public static final c_int IPV6_MULTICAST_LOOP = constant();
    public static final c_int IPV6_UNICAST_HOPS = constant();
    public static final c_int IPV6_V6ONLY = constant();

    // todo: IN6_IS_* macros

    // todo: what is the disposition of these in terms of standards?

    public static final c_int IPV6_TCLASS = constant();

    public static final c_int IP_TOS = constant();
    public static final c_int IP_MULTICAST_IF = constant();
    public static final c_int IP_MULTICAST_TTL = constant();
    public static final c_int IP_MULTICAST_LOOP = constant();
}
