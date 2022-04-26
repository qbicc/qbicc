package org.qbicc.plugin.methodinfo;

import java.util.Objects;

import org.qbicc.graph.literal.Literal;
import org.qbicc.graph.literal.ProgramObjectLiteral;

final class MethodInfo {
    private Literal fileNameSymbolLiteral;
    private Literal methodNameSymbolLiteral;
    private Literal methodDescSymbolLiteral;
    private int typeId;
    private int modifiers;

    MethodInfo(Literal fileSymbolLiteral, Literal methodSymbolLiteral, Literal methodDescSymbolLiteral, int typeId, int modifiers) {
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

    Literal getFileNameSymbolLiteral() {
        return fileNameSymbolLiteral;
    }

    Literal getMethodNameSymbolLiteral() {
        return methodNameSymbolLiteral;
    }

    Literal getMethodDescSymbolLiteral() {
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
