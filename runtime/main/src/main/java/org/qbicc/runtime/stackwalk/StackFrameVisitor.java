package org.qbicc.runtime.stackwalk;

@FunctionalInterface
public interface StackFrameVisitor {
    void visitFrame(int frameIndex, long ip, long sp);
}
