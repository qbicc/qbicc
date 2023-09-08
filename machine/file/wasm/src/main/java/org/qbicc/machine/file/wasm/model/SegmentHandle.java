package org.qbicc.machine.file.wasm.model;

import java.util.NoSuchElementException;

import io.smallrye.common.constraint.Assert;

/**
 * A handle for resolving segments lazily.
 */
public final class SegmentHandle {
    private Segment segment;

    private SegmentHandle() {}

    private SegmentHandle(Segment segment) {
        this.segment = segment;
    }

    /**
     * Get the segment.
     *
     * @return the segment (not {@code null})
     * @throws NoSuchElementException if the handle is not yet initialized
     */
    public Segment segment() {
        Segment segment = this.segment;
        if (segment == null) {
            throw new NoSuchElementException("Segment handle was not initialized");
        }
        return segment;
    }

    /**
     * Initialize this handle with the given segment.
     *
     * @param segment the segment to set this handle to (must not be {@code null})
     * @throws IllegalStateException if the handle is already initialized
     */
    public void initialize(Segment segment) {
        Assert.checkNotNullParam("segment", segment);
        if (this.segment != null) {
            throw new IllegalStateException("Segment handle was already initialized");
        }
        this.segment = segment;
    }

    /**
     * Construct a new initialized instance.
     *
     * @param segment the segment to initialize to (must not be {@code null})
     * @return the initialized segment handle (not {@code null})
     */
    public static SegmentHandle of(Segment segment) {
        Assert.checkNotNullParam("segment", segment);
        return new SegmentHandle(segment);
    }

    /**
     * Construct a new unresolved instance.
     *
     * @return the uninitialized segment handle (not {@code null})
     */
    public static SegmentHandle unresolved() {
        return new SegmentHandle();
    }
}
