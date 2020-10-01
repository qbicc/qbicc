package cc.quarkus.qcc.interpreter;

/**
 *
 */
final class JavaCharArray implements JavaArray {
    private final char[] elements;

    JavaCharArray(final int length) {
        elements = new char[length];
    }

    public int getLength() {
        return elements.length;
    }

    public JavaClass getNestedType() {
        throw new UnsupportedOperationException();
    }

    public boolean getArrayBoolean(final int index) {
        throw new UnsupportedOperationException();
    }

    public int getArrayInt(final int index) {
        return elements[index];
    }

    public long getArrayLong(final int index) {
        throw new UnsupportedOperationException();
    }

    public JavaObject getArrayObject(final int index) {
        throw new UnsupportedOperationException();
    }

    public void putArray(final int index, final boolean value) {
        throw new UnsupportedOperationException();
    }

    public void putArray(final int index, final int value) {
        elements[index] = (char) value;
    }

    public void putArray(final int index, final long value) {
        throw new UnsupportedOperationException();
    }

    public void putArray(final int index, final JavaObject value) {
        throw new UnsupportedOperationException();
    }
}
