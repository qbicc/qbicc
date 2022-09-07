package org.qbicc.runtime.main;

import org.qbicc.runtime.patcher.Patch;

// Temporary class to enable migration

@Patch("java.lang.Thread$_qbicc")
class ThreadSupportQbiccAccess {
    // alias
    static Thread _qbicc_bound_java_thread;
}
