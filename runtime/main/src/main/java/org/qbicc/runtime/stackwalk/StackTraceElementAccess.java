package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.patcher.PatchClass;

@PatchClass(StackTraceElement.class)
class StackTraceElementAccess {
    // alias
    transient Class<?> declaringClassObject;
}
