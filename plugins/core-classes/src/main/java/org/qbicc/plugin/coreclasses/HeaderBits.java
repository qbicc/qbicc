package org.qbicc.plugin.coreclasses;

import io.smallrye.common.constraint.Assert;
import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.UnsignedIntegerType;

/**
 * Header bits reservation API for core object header field.  If the header size is zero, no object header field
 * will be created.
 */
public final class HeaderBits {
    private static final AttachmentKey<HeaderBits> KEY = new AttachmentKey<>();

    private final CompilationContext ctxt;
    private final MutableObjectIntMap<Key> reservations = ObjectIntMaps.mutable.empty();
    private long reservedBits;
    private volatile UnsignedIntegerType headerType;

    private HeaderBits(final CompilationContext ctxt) {
        this.ctxt = ctxt;
    }

    public static HeaderBits get(CompilationContext ctxt) {
        HeaderBits co = ctxt.getAttachment(KEY);
        if (co == null) {
            co = new HeaderBits(ctxt);
            HeaderBits appearing = ctxt.putAttachmentIfAbsent(KEY, co);
            if (appearing != null) {
                co = appearing;
            }
        }
        return co;
    }

    /**
     * Get the header bit index for the given key, reserving a certain number of bits in the header if necessary.
     * Bits are reserved in order from low to high.
     *
     * @param key the key representing the number and identity of the bits to reserve
     * @return the index of the lowest reserved bit
     * @throws IllegalStateException if the key represents unreserved bits but the header field has already been created
     * @throws IllegalArgumentException if there are not enough consecutive free bits in the header for the requested reservation
     */
    public int getHeaderBits(Key key) {
        Assert.checkNotNullParam("key", key);
        synchronized (reservations) {
            if (reservations.containsKey(key)) {
                return reservations.get(key);
            }
            if (headerType != null) {
                throw new IllegalStateException("Too late to reserve header bits");
            }
            // bits 0 to key.bitCount (exclusive) are set
            long bitsToReserve = (1L << key.bitCount) - 1;
            // find the lowest group of header bits that has enough space for the requested bit count
            long bits = reservedBits;
            for (;;) {
                final int pos = Long.numberOfTrailingZeros(Long.lowestOneBit(~bits));
                if (pos == 64) {
                    // no bits are free
                    throw new IllegalArgumentException("Not enough consecutive free header bits to reserve " + key.bitCount + " bit(s)");
                }
                if ((bits & (bitsToReserve << pos)) == 0) {
                    // all are free; reserve them
                    reservedBits |= bitsToReserve << pos;
                    reservations.put(key, pos);
                    return pos;
                }
                bits |= bitsToReserve << pos;
            }
        }
    }

    /**
     * Attempt to reserve a specific set of header bits.
     * If the given key already reserves bits at the given bit position, {@code true} is returned and no other operation is performed.
     *
     * @param key the key representing the number and identity of the bits to reserve
     * @param bitNum the starting bit number to reserve
     * @return {@code true} if the bits were reserved at the given position, or {@code false} if the position already contains reserved bits
     * @throws IllegalStateException if the key represents unreserved bits but the header field has already been created
     * @throws IllegalArgumentException if the key already refers to a different bit position
     */
    public boolean getSpecificHeaderBits(Key key, int bitNum) {
        Assert.checkNotNullParam("key", key);
        Assert.checkMinimumParameter("bitNum", 0, bitNum);
        Assert.checkMaximumParameter("bitNum", 64 - key.bitCount, bitNum);
        synchronized (reservations) {
            if (reservations.containsKey(key)) {
                if (reservations.get(key) == bitNum) {
                    return true;
                }
            } else {
                throw new IllegalArgumentException("Key is already reserved to another bit position");
            }
            if (headerType != null) {
                throw new IllegalStateException("Too late to reserve header bits");
            }
            // the exact bits we want to reserve
            long bitsToReserve = ((1L << key.bitCount) - 1) << bitNum;
            if ((reservedBits & bitsToReserve) == 0) {
                // all are free; reserve them
                reservedBits |= bitsToReserve;
                reservations.put(key, bitNum);
                return true;
            }
        }
        // not all bits are free at the given position
        return false;
    }

    /**
     * Get the header type, rounded up to the nearest power-of-two-bytes. Calling this method locks the header size.
     *
     * @return the header type
     */
    public UnsignedIntegerType getHeaderType() {
        UnsignedIntegerType headerType = this.headerType;
        if (headerType != null) {
            return headerType;
        }
        synchronized (reservations) {
            headerType = this.headerType;
            if (headerType != null) {
                return headerType;
            }
            final long reservedBits = this.reservedBits;
            if (reservedBits == 0) {
                headerType = ctxt.getTypeSystem().getUnsignedInteger8Type();
            } else {
                int bits = Long.numberOfTrailingZeros(Long.highestOneBit(reservedBits));
                // this is the raw bit count; now we find the smallest power of two that is greater than or equal to this
                if (bits <= 8) {
                    headerType = ctxt.getTypeSystem().getUnsignedInteger8Type();
                } else if (bits <= 16) {
                    headerType = ctxt.getTypeSystem().getUnsignedInteger16Type();
                } else if (bits <= 32) {
                    headerType = ctxt.getTypeSystem().getUnsignedInteger32Type();
                } else /* if (bits <= 64) */ {
                    headerType = ctxt.getTypeSystem().getUnsignedInteger64Type();
                }
            }
            this.headerType = headerType;
            return headerType;
        }
    }

    /**
     * A reservation key for a set of header bits.
     */
    public static final class Key {
        private final int bitCount;

        public Key(int bitCount) {
            Assert.checkMinimumParameter("bitCount", 1, bitCount);
            Assert.checkMaximumParameter("bitCount", 64, bitCount);
            this.bitCount = bitCount;
        }
    }
}
