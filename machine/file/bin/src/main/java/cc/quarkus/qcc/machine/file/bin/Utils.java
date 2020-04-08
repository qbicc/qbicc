package cc.quarkus.qcc.machine.file.bin;

/**
 *
 */
public final class Utils {
    private Utils() {
    }

    public static void rangeCheck32(final long value) {
        if (value > 0xFFFF_FFFFL || value < Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Value is out of range for a 32-bit number: 0x" + Long.toHexString(value));
        }
    }

    public static void rangeCheck16(final long value) {
        if (value > 0xFFFF || value < Short.MIN_VALUE) {
            throw new IllegalArgumentException("Value is out of range for a 16-bit number: 0x" + Long.toHexString(value));
        }
    }

    public static void rangeCheck16(final int value) {
        if (value > 0xFFFF || value < Short.MIN_VALUE) {
            throw new IllegalArgumentException("Value is out of range for a 16-bit number: 0x" + Integer.toHexString(value));
        }
    }

    public static void rangeCheck8(final long value) {
        if (value > 0xFF || value < Byte.MIN_VALUE) {
            throw new IllegalArgumentException("Value is out of range for an 8-bit number: 0x" + Long.toHexString(value));
        }
    }

    public static void rangeCheck8(final int value) {
        if (value > 0xFF || value < Byte.MIN_VALUE) {
            throw new IllegalArgumentException("Value is out of range for an 8-bit number: 0x" + Integer.toHexString(value));
        }
    }
}
