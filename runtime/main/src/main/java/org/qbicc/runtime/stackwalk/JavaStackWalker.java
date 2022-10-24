package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.AutoQueued;
import org.qbicc.runtime.Hidden;
import org.qbicc.runtime.NoSafePoint;
import org.qbicc.runtime.NoThrow;
import org.qbicc.runtime.StackObject;

public final class JavaStackWalker extends StackObject {
    private static final int I_ACC_HIDDEN = 1 << 18;

    private final boolean skipHidden;
    private int index = -1;
    private int sourceIndex = -1;
    private int methodInfoIdx = -1;

    @NoSafePoint
    @NoThrow
    public JavaStackWalker(boolean skipHidden) {
        this.skipHidden = skipHidden;
    }

    @NoSafePoint
    @NoThrow
    public JavaStackWalker(JavaStackWalker original) {
        skipHidden = original.skipHidden;
        index = original.index;
        sourceIndex = original.sourceIndex;
        methodInfoIdx = original.methodInfoIdx;
    }

    @NoSafePoint
    @NoThrow
    public boolean next(StackWalker stackWalker) {
        for (;;) {
            for (;;) {
                if (sourceIndex != -1) {
                    sourceIndex = MethodData.getInlinedAtIndex(sourceIndex);
                    break;
                } else if (!stackWalker.next()) {
                    methodInfoIdx = -1;
                    return false;
                } else {
                    // TODO: Mark "base" frames with a flag, to jump over natives; then, -1 is an error (corrupt stack)
                    int ii = MethodData.findInstructionIndex(stackWalker.getIp().longValue());
                    if (ii != -1) {
                        sourceIndex = MethodData.getSourceCodeInfoIndex(ii);
                        break;
                    }
                }
            }
            if (sourceIndex != -1) {
                methodInfoIdx = MethodData.getMethodInfoIndex(sourceIndex);
                if (skipHidden && MethodData.hasAllModifiersOf(methodInfoIdx, I_ACC_HIDDEN)) {
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
    @NoSafePoint
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
    @NoSafePoint
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

    @Hidden
    @AutoQueued
    public static void walkStack(Throwable exceptionObject, JavaStackFrameVisitor visitor) {
        StackWalker sw = new StackWalker();
        JavaStackWalker javaStackWalker = new JavaStackWalker(true);
        while (javaStackWalker.next(sw)) {
            visitor.visitFrame(javaStackWalker.index, javaStackWalker.sourceIndex);
        }
    }

    @NoSafePoint
    @NoThrow
    public int getIndex() {
        return index;
    }

    public String getFrameSourceFileName() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getFileName(methodInfoIdx);
    }

    public String getFrameMethodName() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getMethodName(methodInfoIdx);
    }

    public String getFrameClassName() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getClassName(methodInfoIdx);
    }

    public Class<?> getFrameClass() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getClass(methodInfoIdx);
    }

    public int getFrameLineNumber() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getLineNumber(sourceIndex);
    }

    public int getFrameBytecodeIndex() {
        if (sourceIndex == -1) throw new IllegalStateException();
        return MethodData.getBytecodeIndex(sourceIndex);
    }

    @NoSafePoint
    @NoThrow
    public int getSourceIndex() {
        return sourceIndex;
    }

    @NoSafePoint
    @NoThrow
    public int getMethodInfoIdx() {
        return methodInfoIdx;
    }

    @NoSafePoint
    @NoThrow
    public void reset() {
        methodInfoIdx = -1;
        sourceIndex = -1;
        index = -1;
    }
}
