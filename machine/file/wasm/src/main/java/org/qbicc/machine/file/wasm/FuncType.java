package org.qbicc.machine.file.wasm;

import java.util.List;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public record FuncType(
    List<ValType> parameterTypes,
    List<ValType> resultTypes
) implements Type, Comparable<FuncType> {
    public FuncType {
        Assert.checkNotNullParam("parameterTypes", parameterTypes);
        Assert.checkNotNullParam("resultTypes", resultTypes);
        parameterTypes = List.copyOf(parameterTypes);
        resultTypes = List.copyOf(resultTypes);
    }

    @Override
    public String toString() {
        return parameterTypes + " -> " + resultTypes;
    }

    @Override
    public int compareTo(FuncType o) {
        int res = compareList(parameterTypes, o.parameterTypes);
        if (res == 0) res = compareList(resultTypes, o.resultTypes);
        return res;
    }

    private int compareList(final List<ValType> list1, final List<ValType> list2) {
        int size1 = list1.size();
        int size2 = list2.size();
        int size = Math.min(size1, size2);
        int res;
        for (int i = 0; i < size; i ++) {
            res = ValType.compare(list1.get(i), list2.get(i));
            if (res != 0) {
                return res;
            }
        }
        // the shorter one goes first
        return Integer.compare(size1, size2);
    }

    public static final FuncType EMPTY = new FuncType(List.of(), List.of());

    public static FuncType returning(ValType type) {
        return type.asFuncTypeReturning();
    }
}
