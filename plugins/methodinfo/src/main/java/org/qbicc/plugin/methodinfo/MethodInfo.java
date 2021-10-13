package org.qbicc.plugin.methodinfo;

import org.qbicc.graph.literal.SymbolLiteral;

final class MethodInfo {
    private SymbolLiteral fileNameSymbolLiteral;
    private SymbolLiteral classNameSymbolLiteral;
    private SymbolLiteral methodNameSymbolLiteral;
    private SymbolLiteral methodDescSymbolLiteral;

    MethodInfo(SymbolLiteral fileSymbolLiteral, SymbolLiteral classSymbolLiteral, SymbolLiteral methodSymbolLiteral, SymbolLiteral methodDescSymbolLiteral) {
        this.fileNameSymbolLiteral = fileSymbolLiteral;
        this.classNameSymbolLiteral = classSymbolLiteral;
        this.methodNameSymbolLiteral = methodSymbolLiteral;
        this.methodDescSymbolLiteral = methodDescSymbolLiteral;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return fileNameSymbolLiteral == that.fileNameSymbolLiteral
            && classNameSymbolLiteral == that.classNameSymbolLiteral
            && methodNameSymbolLiteral == that.methodNameSymbolLiteral
            && methodDescSymbolLiteral == that.methodDescSymbolLiteral;
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

    @Override
    public int hashCode() {
        int result = fileNameSymbolLiteral != null ? fileNameSymbolLiteral.hashCode() : 0;
        result = 31 * result + (classNameSymbolLiteral != null ? classNameSymbolLiteral.hashCode() : 0);
        result = 31 * result + (methodNameSymbolLiteral != null ? methodNameSymbolLiteral.hashCode() : 0);
        result = 31 * result + (methodDescSymbolLiteral != null ? methodDescSymbolLiteral.hashCode() : 0);
        return result;
    }
}
