package org.qbicc.plugin.methodinfo;

import org.qbicc.plugin.stringpool.StringId;

final class MethodInfo {
    private StringId fileNameStringId;
    private StringId classNameStringId;
    private StringId methodNameStringId;
    private StringId methodDescStringId;

    MethodInfo(StringId fileStringId, StringId classStringId, StringId methodStringId, StringId methodDescStringId) {
        this.fileNameStringId = fileStringId;
        this.classNameStringId = classStringId;
        this.methodNameStringId = methodStringId;
        this.methodDescStringId = methodDescStringId;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return fileNameStringId == that.fileNameStringId
            && classNameStringId == that.classNameStringId
            && methodNameStringId == that.methodNameStringId
            && methodDescStringId == that.methodDescStringId;
    }

    StringId getFileNameStringId() {
        return fileNameStringId;
    }

    StringId getClassNameStringId() {
        return classNameStringId;
    }

    StringId getMethodNameStringId() {
        return methodNameStringId;
    }

    StringId getMethodDescStringId() {
        return methodDescStringId;
    }

    @Override
    public int hashCode() {
        int result = fileNameStringId != null ? fileNameStringId.hashCode() : 0;
        result = 31 * result + (classNameStringId != null ? classNameStringId.hashCode() : 0);
        result = 31 * result + (methodNameStringId != null ? methodNameStringId.hashCode() : 0);
        result = 31 * result + (methodDescStringId != null ? methodDescStringId.hashCode() : 0);
        return result;
    }
}
