package cc.quarkus.vm.api;

/**
 * A host library which can be registered for loading when the corresponding library name is loaded.
 */
public interface HostLibrary {
    default void onLoad(JavaThread thread) {}

    default void onUnload(JavaThread thread) {}
}
