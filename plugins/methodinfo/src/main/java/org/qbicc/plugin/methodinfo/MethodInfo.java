package org.qbicc.plugin.methodinfo;

import org.qbicc.graph.literal.SymbolLiteral;

import java.util.Objects;

final class MethodInfo {
    private SymbolLiteral fileNameSymbolLiteral;
    private SymbolLiteral classNameSymbolLiteral;
    private SymbolLiteral methodNameSymbolLiteral;
    private SymbolLiteral methodDescSymbolLiteral;
    private int typeId;
    private int modifiers;

    MethodInfo(SymbolLiteral fileSymbolLiteral, SymbolLiteral classSymbolLiteral, SymbolLiteral methodSymbolLiteral, SymbolLiteral methodDescSymbolLiteral, int typeId, int modifiers) {
        this.fileNameSymbolLiteral = fileSymbolLiteral;
        this.classNameSymbolLiteral = classSymbolLiteral;
        this.methodNameSymbolLiteral = methodSymbolLiteral;
        this.methodDescSymbolLiteral = methodDescSymbolLiteral;
        this.typeId = typeId;
        this.modifiers = modifiers;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return Objects.equals(fileNameSymbolLiteral, that.fileNameSymbolLiteral) // fileNameSymbolLiteral can be null, avoid using equals() on it
            && Objects.equals(classNameSymbolLiteral, that.classNameSymbolLiteral)
            && Objects.equals(methodNameSymbolLiteral, that.methodNameSymbolLiteral)
            && Objects.equals(methodDescSymbolLiteral, that.methodDescSymbolLiteral)
            && typeId == that.typeId
            && modifiers == that.modifiers;
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

    int getModifiers() { return modifiers; }

    @Override
    public int hashCode() {
        return Objects.hash(fileNameSymbolLiteral, classNameSymbolLiteral, methodDescSymbolLiteral, methodDescSymbolLiteral, typeId, modifiers);
    }
}
