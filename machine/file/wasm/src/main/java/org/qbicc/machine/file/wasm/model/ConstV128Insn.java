package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.bin.I128;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes a constant value argument.
 */
public record ConstV128Insn(Op.ConstV128 op, long low, long high) implements Insn<Op.ConstV128> {
    public ConstV128Insn {
        Assert.checkNotNullParam("op", op);
    }

    public ConstV128Insn(final Op.ConstV128 op, final long low) {
        this(op, low, 0);
    }

    public ConstV128Insn(final Op.ConstV128 op, final I128 val) {
        this(op, val.low(), val.high());
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.rawLong(low);
        wos.rawLong(high);
    }
}
