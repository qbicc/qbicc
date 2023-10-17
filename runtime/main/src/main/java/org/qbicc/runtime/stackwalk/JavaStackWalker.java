package org.qbicc.runtime.stackwalk;

import static org.qbicc.runtime.CNative.*;
import static org.qbicc.runtime.stackwalk.CallSiteTable.*;

import java.lang.invoke.MethodType;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.ExtModifier;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.SafePoint;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.runtime.StackObject;
import org.qbicc.runtime.main.CompilerIntrinsics;

public final class JavaStackWalker extends StackObject {

    private final boolean skipHidden;
    private int index = -1;
    private ptr<@c_const struct_call_site> call_site_ptr;
    private ptr<@c_const struct_source> source_ptr;

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public JavaStackWalker(boolean skipHidden) {
        this.skipHidden = skipHidden;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public JavaStackWalker(JavaStackWalker original) {
        skipHidden = original.skipHidden;
        index = original.index;
        call_site_ptr = original.call_site_ptr;
        source_ptr = original.source_ptr;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public boolean next(StackWalker stackWalker) {
        for (;;) {
            for (;;) {
                if (source_ptr != null) {
                    source_ptr = getInlinedAt(source_ptr);
                    break;
                } else if (!stackWalker.next()) {
                    return false;
                } else {
                    // TODO: Mark "base" frames with a flag, to jump over natives; then, null is an error (corrupt stack)
                    call_site_ptr = findInsnTableEntry(stackWalker.getIp());
                    if (call_site_ptr != null) {
                        source_ptr = getCallSiteSourceInfo(call_site_ptr);
                        break;
                    }
                }
            }
            if (source_ptr != null) {
                final ptr<@c_const struct_subprogram> method_ptr = getMethodInfo(source_ptr);
                if (skipHidden && methodHasAllModifiersOf(method_ptr, ExtModifier.I_ACC_HIDDEN)) {
                    // try again
                    continue;
                }
                index ++;
                return true;
            }
        }
    }

    @Hidden
    @AutoQueued
    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public static int getFrameCount(Throwable exceptionObject) {
        StackWalker sw = new StackWalker();
        JavaStackWalker javaStackWalker = new JavaStackWalker(true);
        int cnt = 0;
        while (javaStackWalker.next(sw)) {
            cnt ++;
        }
        return cnt;
    }

    /**
     * Get the number of Java stack frames in the current stack.
     *
     * @param skipRawFrames the number of initial raw (native) frames to skip
     * @param skipJavaFrames the number of initial Java frames to skip after any skipped raw frames
     * @param skipHidden {@code true} to skip frames of hidden methods, {@code false} to retain them
     * @return the number of matching frames
     */
    @Hidden
    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public static int getFrameCount(int skipRawFrames, int skipJavaFrames, boolean skipHidden) {
        StackWalker sw = new StackWalker();
        for (int i = 0; i < skipRawFrames; i ++) {
            sw.next();
        }
        JavaStackWalker jsw = new JavaStackWalker(skipHidden);
        for (int i = 0; i < skipJavaFrames; i ++) {
            jsw.next(sw);
        }
        int cnt = 0;
        while (jsw.next(sw)) {
            cnt ++;
        }
        return cnt;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getIndex() {
        return index;
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public String getFrameSourceFileName() {
        return getMethodFileName(getMethodInfo(source_ptr));
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public String getFrameMethodName() {
        return getMethodName(getMethodInfo(source_ptr));
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public MethodType getFrameMethodType() {
        return getMethodType(getMethodInfo(source_ptr));
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public String getFrameClassName() {
        final type_id enclosingType = getEnclosingType(getMethodInfo(source_ptr));
        final ClassAccess clazz = cast(CompilerIntrinsics.getClassFromTypeIdSimple(enclosingType));
        return clazz.name;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public ptr<struct_call_site> getCallSitePtr() {
        return call_site_ptr;
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public ptr<struct_source> getSourcePtr() {
        return source_ptr;
    }

    @SafePoint(SafePointBehavior.NONE)
    @NoThrow
    public Class<?> getFrameClass() {
        return CompilerIntrinsics.getClassFromTypeIdSimple(getEnclosingType(getMethodInfo(source_ptr)));
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getFrameLineNumber() {
        return deref(source_ptr).line.intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getFrameBytecodeIndex() {
        return deref(source_ptr).bci.intValue();
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public int getSourceIndex() {
        return getCallSiteSourceIndex(call_site_ptr);
    }

    @SafePoint(SafePointBehavior.ALLOWED)
    @NoThrow
    public void reset() {
        call_site_ptr = zero();
        source_ptr = zero();
        index = -1;
    }
}
