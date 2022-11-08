package org.qbicc.runtime.bsd;



import static org.qbicc.runtime.stdc.Stdint.*;
import static org.qbicc.runtime.stdc.Time.*;
import static org.qbicc.runtime.CNative.*;

@include("<sys/event.h>")
public class SysEvent {
    public static final class struct_kevent extends object {
        public uintptr_t ident;
        public int16_t filter;
        public uint16_t flags;
        public uint32_t fflags;
        public intptr_t data;
        public void_ptr udata;
    }

    public static final class struct_kevent_ptr extends ptr<SysEvent.struct_kevent> {}

    public static native c_int kqueue();

    public static native c_int kevent(c_int kq, struct_kevent_ptr changelist, c_int nchanges, struct_kevent_ptr eventlist, c_int nevents, const_struct_timespec_ptr timeout);
}
