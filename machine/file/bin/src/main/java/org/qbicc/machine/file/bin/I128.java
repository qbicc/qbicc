package org.qbicc.machine.file.bin;

public record I128(long low, long high) implements Comparable<I128> {
    public I128(long low) {
        this(low, low < 0 ? ~0 : 0);
    }

    @Override
    public int compareTo(I128 other) {
        int res = Long.compare(high, other.high);
        return res != 0 ? res : Long.compareUnsigned(low, other.low);
    }

    public int compareUnsignedTo(I128 other) {
        int res = Long.compareUnsigned(high, other.high);
        return res != 0 ? res : Long.compareUnsigned(low, other.low);
    }

    public int signum() {
        return Long.signum(high);
    }

    public static final I128 ZERO = new I128(0);
}
