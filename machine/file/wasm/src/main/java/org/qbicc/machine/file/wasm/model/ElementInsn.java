package org.qbicc.machine.file.wasm.model;

import java.io.IOException;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction which takes an element as its argument.
 */
public record ElementInsn(Op.Element op, Element element) implements Insn<Op.Element>, Cacheable {
    public ElementInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("element", element);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.u32(encoder.encode(element()));
    }
}
