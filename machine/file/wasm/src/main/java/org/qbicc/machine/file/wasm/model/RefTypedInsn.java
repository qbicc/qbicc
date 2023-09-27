package org.qbicc.machine.file.wasm.model;

import java.io.IOException;
import java.util.EnumMap;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.RefType;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * A reference-typed instruction.
 */
public final class RefTypedInsn implements Insn<Op.RefTyped> {
    private final Op.RefTyped op;
    private final RefType type;

    private RefTypedInsn(Op.RefTyped op, RefType type) {
        this.op = op;
        this.type = type;
    }

    @Override
    public Op.RefTyped op() {
        return op;
    }

    public RefType type() {
        return type;
    }

    @Override
    public void writeTo(WasmOutputStream wos, Encoder encoder) throws IOException {
        wos.op(op);
        wos.type(type);
    }

    private static final EnumMap<Op.RefTyped, EnumMap<RefType, RefTypedInsn>> ALL;

    public static RefTypedInsn forOpAndType(Op.RefTyped op, RefType type) {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("type", type);
        return ALL.get(op).get(type);
    }

    static {
        EnumMap<Op.RefTyped, EnumMap<RefType, RefTypedInsn>> map = new EnumMap<>(Op.RefTyped.class);
        for (Op.RefTyped refTyped : Op.RefTyped.values()) {
            EnumMap<RefType, RefTypedInsn> innerMap = new EnumMap<>(RefType.class);
            map.put(refTyped, innerMap);
            for (RefType refType : RefType.values()) {
                innerMap.put(refType, new RefTypedInsn(refTyped, refType));
            }
        }
        ALL = map;
    }
}
