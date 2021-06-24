package org.qbicc.plugin.methodinfo;

final class MethodInfo {
    private int fileNameIndex;
    private int classNameIndex;
    private int methodNameIndex;
    private int methodDescIndex;

    MethodInfo(int fileIndex, int classIndex, int methodIndex, int methodDescIndex) {
        this.fileNameIndex = fileIndex;
        this.classNameIndex = classIndex;
        this.methodNameIndex = methodIndex;
        this.methodDescIndex = methodDescIndex;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return fileNameIndex == that.fileNameIndex
            && classNameIndex == that.classNameIndex
            && methodNameIndex == that.methodNameIndex
            && methodDescIndex == that.methodDescIndex;
    }

    int getFileNameIndex() {
        return fileNameIndex;
    }

    int getClassNameIndex() {
        return classNameIndex;
    }

    int getMethodNameIndex() {
        return methodNameIndex;
    }

    int getMethodDescIndex() {
        return methodDescIndex;
    }

    @Override
    public int hashCode() {
        int result = fileNameIndex;
        result = 31 * result + classNameIndex;
        result = 31 * result + methodNameIndex;
        result = 31 * result + methodDescIndex;
        return result;
    }
}
