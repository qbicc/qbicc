package org.qbicc.plugin.patcher;

import org.qbicc.type.descriptor.Descriptor;

abstract class MemberPatchInfo {
    private final int index;
    private final int modifiers;

    MemberPatchInfo(int index, int modifiers) {
        this.index = index;
        this.modifiers = modifiers;
    }

    int getIndex() {
        return index;
    }

    int getAdditionalModifiers() {
        return modifiers;
    }

    abstract Descriptor getDescriptor();
}
