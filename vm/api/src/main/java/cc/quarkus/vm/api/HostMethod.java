package cc.quarkus.vm.api;

/**
 * A host implementation of a native method.
 */
public interface HostMethod {
    /**
     * Called to invoke a host method.
     *
     * @param currentThread the current thread
     * @param target the invocation target (might be a class or object)
     * @param args the arguments
     * @return the return value or {@code null}
     */
    JavaObject invoke(JavaThread currentThread, JavaObject target, Object... args);
}
