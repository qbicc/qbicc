package cc.quarkus.qcc.runtime;

/**
 * A pin of a GC-able reference object. Holds the object in one memory location for the duration of the pin.
 *
 * @param <T> the type of the object being pinned
 */
public final class Pin<T> extends StackObject implements AutoCloseable {
    private final T obj;

    Pin(final T obj) {
        this.obj = obj;
    }

    /**
     * Create a "fast" pin.  Such pins should be truly "fast" to avoid garbage collection delays and related problems.
     * The implementation may, for example, suspend all copying collection for the duration of the pin.
     *
     * @param obj the object to pin
     * @param <T> the object type
     * @return the pin of the object which <em>must</em> be subsequently released
     */
    public static native <T> Pin<T> fast(T obj);

    /**
     * Create a "slow" pin.  Such pins may be retained for arbitrary amounts of time, but <em>should</em> be
     * eventually released in most cases to avoid memory leaks.  The implementation may, for example, cause the
     * pinned object to be promoted to non-copying heap generation.
     *
     * @param obj the object to pin
     * @param <T> the object type
     * @return the pin of the object which <em>must</em> be subsequently released
     */
    public static native <T> Pin<T> slow(T obj);

    public T getPinnedObject() {
        return obj;
    }

    /**
     * Explicitly unpin the object.
     */
    public native void close();

    protected void destroy() {
        close();
    }
}
