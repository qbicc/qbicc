package org.qbicc.plugin.coreclasses;

import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.UnsignedIntegerType;

/**
 * Header bits reservation API for core object header field.  If the header size is zero, no object header field
 * will be created.
 */
public final class HeaderBits {
    private static final AttachmentKey<HeaderBits> KEY = new AttachmentKey<>();

    /**
     * Reserved bit state. High bit is set when the number of bits is queried to lock the set.
     */
    private final AtomicInteger reservedBitState = new AtomicInteger();
    private final CompilationContext ctxt;

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
     * Reserve a certain number of bits in the header. Bits are reserved in order from low to high.
     *
     * @param bits the number of bits to reserve
     * @return the index of the lowest reserved bit
     */
    public int reserveHeaderBits(int bits) {
        Assert.checkMinimumParameter("bits", 1, bits);
        int oldVal, newVal;
        do {
            oldVal = reservedBitState.get();
            if (oldVal < 0) {
                throw new IllegalStateException("Too late to reserve header bits");
            }
            newVal = oldVal + bits;
        } while (! reservedBitState.compareAndSet(oldVal, newVal));
        return oldVal;
    }

    /**
     * Get the header type, rounded up to the nearest power-of-two-bytes. Calling this method locks the header size.
     *
     * @return the header type
     */
    public UnsignedIntegerType getHeaderType() {
        int bits, newBits;
        do {
            bits = reservedBitState.get();
            if (bits >= 0) {
                // not locked yet
                newBits = bits | (1 << 31);
            } else {
                bits &= 0x7FFF_FFFF;
                break;
            }
        } while (! reservedBitState.compareAndSet(bits, newBits));
        // this is the raw byte count; now we find the smallest power of two that is greater than or equal to this
        if (bits <= 8) {
            return ctxt.getTypeSystem().getUnsignedInteger8Type();
        } else if (bits <= 16) {
            return ctxt.getTypeSystem().getUnsignedInteger16Type();
        } else if (bits <= 32) {
            return ctxt.getTypeSystem().getUnsignedInteger32Type();
        } else if (bits <= 64) {
            return ctxt.getTypeSystem().getUnsignedInteger64Type();
        } else {
            throw new IllegalArgumentException("Too many header bits");
        }
    }
}
