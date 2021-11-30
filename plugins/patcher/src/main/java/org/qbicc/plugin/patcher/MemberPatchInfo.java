package org.qbicc.plugin.patcher;

import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.Descriptor;

abstract class MemberPatchInfo {
    private final int index;
    private final int modifiers;
    private final String internalName;
    private final Annotation annotation;

    MemberPatchInfo(int index, int modifiers, String internalName, Annotation annotation) {
        this.index = index;
        this.modifiers = modifiers;
        this.internalName = internalName;
        this.annotation = annotation;
    }

    int getIndex() {
        return index;
    }

    int getAdditionalModifiers() {
        return modifiers;
    }

    abstract Descriptor getDescriptor();

    String getInternalName() {
        return internalName;
    }

    Annotation getAnnotation() {
        return annotation;
    }
}
