package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.Hidden;
import org.qbicc.type.definition.classfile.ClassFile;

public class JavaStackWalker implements StackFrameVisitor {
    private Throwable exceptionObject;
    private JavaStackFrameVisitor visitor;
    private int javaFrameCount;

    private JavaStackWalker(Throwable exceptionObject, JavaStackFrameVisitor visitor) {
        this.exceptionObject = exceptionObject;
        this.visitor = visitor;
        this.javaFrameCount = 0;
    }

    @Hidden
    public static int getFrameCount(Throwable exceptionObject) {
        JavaStackWalker javaStackWalker = new JavaStackWalker(exceptionObject, new NopVisitor());
        StackWalker.walkStack(javaStackWalker);
        return javaStackWalker.javaFrameCount;
    }

    @Hidden
    public static void walkStack(Throwable exceptionObject, JavaStackFrameVisitor visitor) {
        StackFrameVisitor javaStackWalker = new JavaStackWalker(exceptionObject, visitor);
        StackWalker.walkStack(javaStackWalker);
    }

    private boolean skipFrame(int scIndex, boolean isTopFrame) {
        int minfoIndex = MethodData.getMethodInfoIndex(scIndex);
        String methodName = MethodData.getMethodName(minfoIndex);
        String className = MethodData.getClassName(minfoIndex);

        if (isTopFrame) {
            // if this is top frame, skip it if it is for excepption constructor or "fillInStackTrace" method
            if (exceptionObject.getClass().getName().equals(className)) {
                if (methodName.equals("<init>") || methodName.equals("fillInStackTrace")) {
                    return true;
                }
            }
        }
        if (MethodData.hasAllModifiersOf(minfoIndex, ClassFile.I_ACC_HIDDEN)) {
            return true;
        }
        return false;
    }

    public void visitFrame(int frameIndex, long ip, long sp) {
        int index = MethodData.findInstructionIndex(ip);
        if (index != -1) {
            int scIndex = MethodData.getSourceCodeInfoIndex(index);
            boolean topFrame = (javaFrameCount == 0);
            if (!skipFrame(scIndex, topFrame)) {
                visitor.visitFrame(javaFrameCount, scIndex);
                javaFrameCount += 1;
            }
            int inlinedAtIndex = MethodData.getInlinedAtIndex(scIndex);
            while (inlinedAtIndex != -1) {
                topFrame = (javaFrameCount == 0);
                if (!skipFrame(scIndex, topFrame)) {
                    visitor.visitFrame(javaFrameCount, inlinedAtIndex);
                    javaFrameCount += 1;
                }
                inlinedAtIndex = MethodData.getInlinedAtIndex(inlinedAtIndex);
            }
        } else {
            // skip this frame; probably a native frame
        }
    }

    private static class NopVisitor implements JavaStackFrameVisitor {
        @Override
        public void visitFrame(int frameIndex, int scIndex) { /* no-op */ }
    }
}
