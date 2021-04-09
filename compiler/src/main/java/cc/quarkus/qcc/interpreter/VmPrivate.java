package org.qbicc.interpreter;

final class VmPrivate {
    static final ThreadLocal<VmThread> CURRENT_THREAD = new ThreadLocal<>();
}
