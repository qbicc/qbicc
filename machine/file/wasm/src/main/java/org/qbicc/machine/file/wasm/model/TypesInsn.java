package org.qbicc.machine.file.wasm.model;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Assert;
import org.qbicc.machine.file.wasm.Op;
import org.qbicc.machine.file.wasm.ValType;
import org.qbicc.machine.file.wasm.stream.WasmOutputStream;

/**
 * An instruction that operates on a list of types.
 */
public record TypesInsn(Op.Types op, List<ValType> types) implements Insn<Op.Types> {
    public TypesInsn {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("types", types);
        Assert.checkNotEmptyParam("types", types);
    }

    public void writeTo(final WasmOutputStream wos, final Encoder encoder) throws IOException {
        wos.op(op);
        wos.typeVec(types);
    }

    private static final EnumMap<Op.Types, Map<ValType, TypesInsn>> ONE_TYPE;

    /**
     * Return a cached instruction instance for single-typed cases.
     *
     * @param op the operation (must not be {@code null})
     * @param type the value type (must not be {@code null})
     * @return the cached instruction (not {@code null})
     */
    public static TypesInsn forOpAndType(Op.Types op, ValType type) {
        Assert.checkNotNullParam("op", op);
        Assert.checkNotNullParam("type", type);
        return ONE_TYPE.get(op).get(type);
    }

    static {
        EnumMap<Op.Types, Map<ValType, TypesInsn>> map = new EnumMap<>(Op.Types.class);
        for (Op.Types op : Op.Types.values()) {
            HashMap<ValType, TypesInsn> subMap = new HashMap<>();
            for (ValType type : ValType.values()) {
                subMap.put(type, new TypesInsn(op, List.of(type)));
            }
            map.put(op, Map.copyOf(subMap));
        }
        ONE_TYPE = map;
    }
}
