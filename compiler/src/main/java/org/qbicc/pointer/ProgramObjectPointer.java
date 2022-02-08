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

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
