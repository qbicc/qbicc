package org.qbicc.runtime.posix;

import org.qbicc.runtime.Build;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysSocket.*;
import static org.qbicc.runtime.stdc.Stdint.*;

@define(value = "_POSIX_C_SOURCE", as = "200809L")
@define(value = "_DARWIN_C_SOURCE", when = Build.Target.IsApple.class)
@include("<netdb.h>")
public class Netdb {

    public static final class struct_hostent extends object {
        public char_ptr h_name;
        public ptr<char_ptr> h_aliases;
        public c_int h_addrtype;
        public c_int h_length;
        public ptr<char_ptr> h_addr_list;
    }

    public static final class struct_netent extends object {
        public char_ptr n_name;
        public ptr<char_ptr> n_aliases;
        public c_int n_addrtype;
        public uint32_t n_net;
    }

    public static final class struct_servent extends object {
        public char_ptr s_name;
        public ptr<char_ptr> s_aliases;
        public c_int s_port;
        public char_ptr s_proto;
    }

    public static final class sturct_protoent extends object {
        public char_ptr p_name;
        public ptr<char_ptr> p_aliases;
        public c_int p_proto;
    }

    public static final class struct_addrinfo extends object {
        public c_int ai_flags;
        public c_int ai_family;
        public c_int ai_socktype;
        public c_int ai_protocol;
        public socklen_t ai_addrlen;
        public char_ptr ai_canonname;
        public ptr<struct_sockaddr> ai_addr;
        public ptr<struct_addrinfo> ai_next;
    }
    public static final class struct_addrinfo_ptr extends ptr<struct_addrinfo> {}

    public static final c_int AI_PASSIVE = constant();
    public static final c_int AI_CANONNAME = constant();
    public static final c_int AI_NUMERICHOST = constant();
    public static final c_int AI_NUMERICSERV = constant();
    public static final c_int AI_V4MAPPED = constant();
    public static final c_int AI_ALL = constant();
    public static final c_int AI_ADDRCONFIG = constant();

    public static native c_int getaddrinfo(const_char_ptr hostname, const_char_ptr servname,
                                           ptr<@c_const struct_addrinfo> hints, ptr<ptr<struct_addrinfo>> res);

    public static native void freeaddrinfo(ptr<struct_addrinfo> ai);
}
