package org.qbicc.runtime.stackwalk;

import org.qbicc.runtime.AutoQueued;

import java.util.Arrays;

@SuppressWarnings("unused")
public class JavaStackFrameCache implements JavaStackFrameVisitor {
    private final int[] sourceCodeIndexList;

    @AutoQueued
    JavaStackFrameCache(final int frameCount) {
        this.sourceCodeIndexList = new int[frameCount];
    }

    @AutoQueued
    public Object getSourceCodeIndexList() {
        return Arrays.copyOf(sourceCodeIndexList, sourceCodeIndexList.length);
    }

    @Override
    @AutoQueued
    public void visitFrame(final int frameIndex, final int scIndex) {
        sourceCodeIndexList[frameIndex] = scIndex;
    }
}
