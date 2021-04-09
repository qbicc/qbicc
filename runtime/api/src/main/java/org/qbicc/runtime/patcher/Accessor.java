package org.qbicc.runtime.patcher;

/**
 * A replacement accessor for some field. The implemented accessor method should match the type of the field.
 *
 * @param <T> the field type; for primitive fields, the corresponding primitive wrapper type and implement the
 *        primitive version of the corresponding accessor method(s)
 */
public interface Accessor<T> {
    default boolean getAsBoolean() {
        throw new UnsupportedOperationException();
    }

    default byte getAsByte() {
        throw new UnsupportedOperationException();
    }

    default short getAsShort() {
        throw new UnsupportedOperationException();
    }

    default int getAsInt() {
        throw new UnsupportedOperationException();
    }

    default long getAsLong() {
        throw new UnsupportedOperationException();
    }

    default float getAsFloat() {
        throw new UnsupportedOperationException();
    }

    default double getAsDouble() {
        throw new UnsupportedOperationException();
    }

    default T get() {
        throw new UnsupportedOperationException();
    }

    default void set(boolean value) {
        throw new UnsupportedOperationException();
    }

    default void set(byte value) {
        throw new UnsupportedOperationException();
    }

    default void set(short value) {
        throw new UnsupportedOperationException();
    }

    default void set(int value) {
        throw new UnsupportedOperationException();
    }

    default void set(long value) {
        throw new UnsupportedOperationException();
    }

    default void set(float value) {
        throw new UnsupportedOperationException();
    }

    default void set(double value) {
        throw new UnsupportedOperationException();
    }

    default void set(T value) {
        throw new UnsupportedOperationException();
    }
}
