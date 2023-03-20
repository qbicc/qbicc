package org.qbicc.runtime.posix;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.posix.SysTypes.*;
import static org.qbicc.runtime.stdc.Stddef.*;
import static org.qbicc.runtime.stdc.Stdio.*;

@include("<stdio.h>")
@define(value = "_POSIX_C_SOURCE", as = "200809L")
public class Stdio {
    public static native ssize_t getline(ptr<ptr<c_char>> linep, ptr<size_t> linecap, ptr<FILE> stream);
}
