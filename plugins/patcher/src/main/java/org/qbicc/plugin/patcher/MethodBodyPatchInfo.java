package org.qbicc.plugin.patcher;

import org.qbicc.type.definition.MethodBodyFactory;

final class MethodBodyPatchInfo {
    private final MethodBodyFactory methodBodyFactory;
    private final int index;

    MethodBodyPatchInfo(MethodBodyFactory methodBodyFactory, int index) {
        this.methodBodyFactory = methodBodyFactory;
        this.index = index;
    }

    public MethodBodyFactory getMethodBodyFactory() {
        return methodBodyFactory;
    }

    public int getIndex() {
        return index;
    }
}
