package org.qbicc.plugin.methodinfo;

import org.qbicc.graph.literal.SymbolLiteral;

import java.util.Objects;

final class MethodInfo {
    private SymbolLiteral fileNameSymbolLiteral;
    private SymbolLiteral classNameSymbolLiteral;
    private SymbolLiteral methodNameSymbolLiteral;
    private SymbolLiteral methodDescSymbolLiteral;
    private int typeId;

    MethodInfo(SymbolLiteral fileSymbolLiteral, SymbolLiteral classSymbolLiteral, SymbolLiteral methodSymbolLiteral, SymbolLiteral methodDescSymbolLiteral, int typeId) {
        this.fileNameSymbolLiteral = fileSymbolLiteral;
        this.classNameSymbolLiteral = classSymbolLiteral;
        this.methodNameSymbolLiteral = methodSymbolLiteral;
        this.methodDescSymbolLiteral = methodDescSymbolLiteral;
        this.typeId = typeId;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return Objects.equals(fileNameSymbolLiteral, that.fileNameSymbolLiteral) // fileNameSymbolLiteral can be null, avoid using equals() on it
            && classNameSymbolLiteral.equals(that.classNameSymbolLiteral)
            && methodNameSymbolLiteral.equals(that.methodNameSymbolLiteral)
            && methodDescSymbolLiteral.equals(that.methodDescSymbolLiteral);
    }

    SymbolLiteral getFileNameSymbolLiteral() {
        return fileNameSymbolLiteral;
    }

    SymbolLiteral getClassNameSymbolLiteral() {
        return classNameSymbolLiteral;
    }

    SymbolLiteral getMethodNameSymbolLiteral() {
        return methodNameSymbolLiteral;
    }

    SymbolLiteral getMethodDescSymbolLiteral() {
        return methodDescSymbolLiteral;
    }

    int getTypeId() {
        return typeId;
    }

    @Override
    public int hashCode() {
        int result = fileNameSymbolLiteral != null ? fileNameSymbolLiteral.hashCode() : 0;
        result = 31 * result + (classNameSymbolLiteral != null ? classNameSymbolLiteral.hashCode() : 0);
        result = 31 * result + (methodNameSymbolLiteral != null ? methodNameSymbolLiteral.hashCode() : 0);
        result = 31 * result + (methodDescSymbolLiteral != null ? methodDescSymbolLiteral.hashCode() : 0);
        return result;
    }
}
