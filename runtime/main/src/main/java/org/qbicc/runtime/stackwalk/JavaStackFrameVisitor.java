package org.qbicc.runtime.stackwalk;

@FunctionalInterface
public interface JavaStackFrameVisitor {
    void visitFrame(int frameIndex, int scIndex);
}
