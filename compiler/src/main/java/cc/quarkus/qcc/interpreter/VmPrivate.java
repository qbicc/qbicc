package cc.quarkus.qcc.interpreter;

final class VmPrivate {
    static final ThreadLocal<VmThread> CURRENT_THREAD = new ThreadLocal<>();
}
