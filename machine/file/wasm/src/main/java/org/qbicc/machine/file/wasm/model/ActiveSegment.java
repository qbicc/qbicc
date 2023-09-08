package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;

/**
 *
 */
public record ActiveSegment(Data data, InsnSeq init) implements Segment {
    public ActiveSegment {
        Assert.checkNotNullParam("data", data);
        Assert.checkNotNullParam("init", init);
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
