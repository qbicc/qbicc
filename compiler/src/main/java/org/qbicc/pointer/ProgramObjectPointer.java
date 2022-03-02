package org.qbicc.pointer;

import org.qbicc.object.ProgramObject;

/**
 * A pointer to a global program object.
 */
public final class ProgramObjectPointer extends RootPointer {
    private final ProgramObject programObject;

    ProgramObjectPointer(ProgramObject programObject) {
        super(programObject.getSymbolType());
        this.programObject = programObject;
    }

    public static ProgramObjectPointer of(final ProgramObject programObject) {
        return programObject.getOrCreatePointer(ProgramObjectPointer::new);
    }

    public ProgramObject getProgramObject() {
        return programObject;
    }

    @Override
    public String getRootSymbolIfExists() {
        return programObject.getName();
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + programObject.getName().hashCode();
    }

    @Override
    public boolean equals(RootPointer other) {
        return other instanceof ProgramObjectPointer pop && equals(pop);
    }

    public boolean equals(ProgramObjectPointer other) {
        return this == other || super.equals(other) && programObject.getName().equals(other.programObject.getName());
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(programObject.getName());
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
