package org.qbicc.plugin.patcher;

import org.qbicc.type.annotation.Annotation;

abstract class ExecutableMemberPatchInfo extends MemberPatchInfo{

    ExecutableMemberPatchInfo(int index, int modifiers, String internalName, Annotation annotation) {
        super(index, modifiers, internalName, annotation);
    }

}
