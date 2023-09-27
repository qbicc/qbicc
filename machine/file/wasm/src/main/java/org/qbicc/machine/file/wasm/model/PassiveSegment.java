package org.qbicc.machine.file.wasm.model;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Data;

/**
 *
 */
public record PassiveSegment(String name, Data data) implements Segment {
    public PassiveSegment {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("data", data);
    }

    public PassiveSegment(Data data) {
        this("", data);
    }
}
