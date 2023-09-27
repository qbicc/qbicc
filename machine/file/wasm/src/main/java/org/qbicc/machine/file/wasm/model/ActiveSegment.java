package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;

/**
 *
 */
public record ActiveSegment(String name, Data data, InsnSeq offset) implements Segment {
    public ActiveSegment {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("data", data);
        Assert.checkNotNullParam("offset", offset);
    }

    public ActiveSegment(Data data, InsnSeq offset) {
        this("", data, offset);
    }
}
