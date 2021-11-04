package org.qbicc.object;

import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.type.FunctionType;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A function definition.
 */
public final class Function extends SectionObject {
    public static final int FN_NO_RETURN = 1 << 0;
    public static final int FN_NO_SIDE_EFFECTS = 1 << 1;

    private final int fnFlags;
    private volatile MethodBody body;
    private volatile FunctionDeclaration declaration;

    Function(final ExecutableElement originalElement, final String name, final SymbolLiteral literal, int fnFlags) {
        super(originalElement, name, literal);
        this.fnFlags = fnFlags;
    }

    public ExecutableElement getOriginalElement() {
        return (ExecutableElement) super.getOriginalElement();
    }

    public FunctionType getType() {
        return (FunctionType) super.getType();
    }

    public MethodBody getBody() {
        return body;
    }

    public void replaceBody(final MethodBody body) {
        this.body = body;
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

    public FunctionDeclaration getDeclaration() {
        FunctionDeclaration declaration = this.declaration;
        if (declaration == null) {
            synchronized (this) {
                declaration = this.declaration;
                if (declaration == null) {
                    declaration = this.declaration = new FunctionDeclaration(getOriginalElement(), name, literal);
                }
            }
        }
        return declaration;
    }

    public static int getFunctionFlags(ExecutableElement element) {
        int flags = 0;
        if (element.hasAllModifiersOf(ClassFile.I_ACC_NO_SIDE_EFFECTS)) {
            flags |= Function.FN_NO_SIDE_EFFECTS;
        } else if (element.hasAllModifiersOf(ClassFile.I_ACC_NO_RETURN)) {
            flags |= Function.FN_NO_RETURN;
        }
        return flags;
    }
}
