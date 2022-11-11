package org.qbicc.object;

import static org.qbicc.object.Function.*;

import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function declaration.
 */
public final class FunctionDeclaration extends Declaration {
    private final int fnFlags;

    FunctionDeclaration(final ExecutableElement originalElement, ProgramModule programModule, final String name, final FunctionType functionType, final int fnFlags) {
        super(originalElement, programModule, name, functionType);
        this.fnFlags = fnFlags;
    }

    FunctionDeclaration(final Function original) {
        super(original);
        this.fnFlags = original.getFlags();
    }

    public int getFlags() {
        return fnFlags;
    }

    public boolean isNoReturn() {
        return (fnFlags & FN_NO_RETURN) != 0;
    }

    public boolean isNoSideEffects() {
        return (fnFlags & FN_NO_SIDE_EFFECTS) != 0;
    }

    public boolean isNoSafePoints() {
        return (fnFlags & FN_NO_SAFEPOINTS) != 0;
    }

    public boolean isNoThrow() {
        return (fnFlags & FN_NO_THROW) != 0;
    }

    @Override
    public FunctionDeclaration getDeclaration() {
        return this;
    }

    public FunctionType getValueType() {
        return (FunctionType) super.getValueType();
    }
}
