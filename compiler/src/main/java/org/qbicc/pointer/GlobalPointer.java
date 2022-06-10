package org.qbicc.pointer;

import org.qbicc.type.definition.element.GlobalVariableElement;

/**
 * A pointer to a global variable.
 */
public final class GlobalPointer extends RootPointer {
    private final GlobalVariableElement globalVariable;

    GlobalPointer(GlobalVariableElement globalVariable) {
        super(globalVariable.getType().getPointer());
        this.globalVariable = globalVariable;
    }

    public static GlobalPointer of(final GlobalVariableElement fieldElement) {
        return fieldElement.getOrCreatePointer(GlobalPointer::new);
    }

    public GlobalVariableElement getGlobalVariable() {
        return globalVariable;
    }

    @Override
    public long getRootByteOffset() {
        return globalVariable.getOffset();
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + globalVariable.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof GlobalPointer gp && equals(gp);
    }

    public boolean equals(final GlobalPointer other) {
        return super.equals(other) && globalVariable == other.globalVariable;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(globalVariable.getName());
    }

    @Override
    public <T, R> R accept(Visitor<T, R> visitor, T t) {
        return visitor.visit(t, this);
    }
}
