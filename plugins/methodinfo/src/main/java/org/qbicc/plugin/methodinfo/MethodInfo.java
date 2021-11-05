package org.qbicc.plugin.methodinfo;

import java.util.Objects;

import org.qbicc.graph.literal.ProgramObjectLiteral;

final class MethodInfo {
    private ProgramObjectLiteral fileNameSymbolLiteral;
    private ProgramObjectLiteral methodNameSymbolLiteral;
    private ProgramObjectLiteral methodDescSymbolLiteral;
    private int typeId;
    private int modifiers;

    MethodInfo(ProgramObjectLiteral fileSymbolLiteral, ProgramObjectLiteral methodSymbolLiteral, ProgramObjectLiteral methodDescSymbolLiteral, int typeId, int modifiers) {
        this.fileNameSymbolLiteral = fileSymbolLiteral;
        this.methodNameSymbolLiteral = methodSymbolLiteral;
        this.methodDescSymbolLiteral = methodDescSymbolLiteral;
        this.typeId = typeId;
        this.modifiers = modifiers;
    }

    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        MethodInfo that = (MethodInfo) other;
        return Objects.equals(fileNameSymbolLiteral, that.fileNameSymbolLiteral)
            && Objects.equals(methodNameSymbolLiteral, that.methodNameSymbolLiteral)
            && Objects.equals(methodDescSymbolLiteral, that.methodDescSymbolLiteral)
            && typeId == that.typeId
            && modifiers == that.modifiers;
    }

    ProgramObjectLiteral getFileNameSymbolLiteral() {
        return fileNameSymbolLiteral;
    }

    ProgramObjectLiteral getMethodNameSymbolLiteral() {
        return methodNameSymbolLiteral;
    }

    ProgramObjectLiteral getMethodDescSymbolLiteral() {
        return methodDescSymbolLiteral;
    }

    int getTypeId() {
        return typeId;
    }

    int getModifiers() { return modifiers; }

    @Override
    public int hashCode() {
        return Objects.hash(fileNameSymbolLiteral, methodDescSymbolLiteral, methodDescSymbolLiteral, typeId, modifiers);
    }
}
