package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;

/**
 *
 */
public record PassiveSegment(Data data) implements Segment {
    public PassiveSegment {
        Assert.checkNotNullParam("data", data);
    }
}
