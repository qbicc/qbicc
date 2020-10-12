package cc.quarkus.qcc.graph;

/**
 * A special type representing a return address for bytecode {@code jsr}/{@code ret} processing.
 */
public final class ReturnAddressType implements Type {
    static final ReturnAddressType INSTANCE = new ReturnAddressType();

    private ReturnAddressType() {}

    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
    }

    public boolean isAssignableFrom(final Type otherType) {
        return otherType == this;
    }

    public int getSourceLine() {
        throw new UnsupportedOperationException();
    }

    public void setSourceLine(final int sourceLine) {
        throw new UnsupportedOperationException();
    }

    public int getBytecodeIndex() {
        throw new UnsupportedOperationException();
    }

    public void setBytecodeIndex(final int bytecodeIndex) {
        throw new UnsupportedOperationException();
    }
}
