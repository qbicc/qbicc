package org.qbicc.plugin.patcher;

import org.qbicc.type.annotation.Annotation;

import java.util.List;

abstract class ExecutableMemberPatchInfo extends MemberPatchInfo {

    ExecutableMemberPatchInfo(int index, int modifiers, String internalName, Annotation annotation, List<Annotation> addedAnnotations) {
        super(index, modifiers, internalName, annotation, addedAnnotations);
    }
}
