package org.qbicc.runtime.bsd;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;

@include("<sys/proc.h>")
public class SysProc {
    public static final class struct_extern_proc extends object {
        public void_ptr p_un; // union { struct { proc*; proc* } p_st1; timeval __p_starttime }
        public pid_t p_pid;
    }
}
