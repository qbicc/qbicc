package cc.quarkus.c_native.api;

import cc.quarkus.qcc.api.StackObject;

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

    public static native <T> Pin<T> create(T obj);

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
