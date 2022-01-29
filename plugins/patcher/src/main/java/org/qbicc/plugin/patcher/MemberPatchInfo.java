package org.qbicc.plugin.patcher;

import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.descriptor.Descriptor;

import java.util.List;

abstract class MemberPatchInfo {
    private final int index;
    private final int modifiers;
    private final String internalName;
    private final Annotation annotation;
    private final List<Annotation> addedAnnotations;

    MemberPatchInfo(int index, int modifiers, String internalName, Annotation annotation, List<Annotation> addedAnnotations) {
        this.index = index;
        this.modifiers = modifiers;
        this.internalName = internalName;
        this.annotation = annotation;
        this.addedAnnotations = addedAnnotations;
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

    List<Annotation> getAddedAnnotations() {
        return addedAnnotations;
    }
}
