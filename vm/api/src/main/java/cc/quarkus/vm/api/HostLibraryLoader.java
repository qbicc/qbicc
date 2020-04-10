package cc.quarkus.vm.api;

/**
 * The host's library loader.
 */
public interface HostLibraryLoader {
    /**
     * Attempt to load a library with the given name.
     *
     * @param name the library name to load
     * @return the host library or {@code null} if no library was found
     */
    HostLibrary loadLibrary(String name);
}
