package org.qbicc.runtime.stackwalker;

public interface StackFrameVisitor {
    boolean visitFrame(long ip, long sp);
}
