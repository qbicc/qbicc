package org.qbicc.runtime.stackwalk;

import java.util.Arrays;

public class JavaStackFrameCache implements JavaStackFrameVisitor {
    private final int sourceCodeIndexList[];

    JavaStackFrameCache(final int frameCount) {
        this.sourceCodeIndexList = new int[frameCount];
    }

    public Object getSourceCodeIndexList() {
        return Arrays.copyOf(sourceCodeIndexList, sourceCodeIndexList.length);
    }

    @Override
    public void visitFrame(final int frameIndex, final int scIndex) {
        sourceCodeIndexList[frameIndex] = scIndex;
    }
}
