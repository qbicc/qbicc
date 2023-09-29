package org.qbicc.pointer;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FunctionElement;

/**
 * A pointer to a function before it is lowered to a program object.
 */
public final class FunctionPointer extends RootPointer implements ExecutableElementPointer {
    private final FunctionElement function;

    FunctionPointer(FunctionElement function) {
        super(function.getType().getPointer());
        this.function = function;
    }

    public static FunctionPointer of(final FunctionElement methodElement) {
        return methodElement.getOrCreatePointer(FunctionPointer::new);
    }

    public FunctionElement getExecutableElement() {
        return function;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + function.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof FunctionPointer smp && equals(smp);
    }

    public boolean equals(final FunctionPointer other) {
        return super.equals(other) && function == other.function;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(function.getEnclosingType().getInternalName()).append('#').append(function.getName()).append(function.getDescriptor());
    }

    public boolean isNoThrow() {
        return getExecutableElement().hasNoModifiersOf(ClassFile.I_ACC_NO_THROW);
    }

    @Override
    public boolean isNoSafePoints() {
        return getExecutableElement().hasNoModifiersOf(ClassFile.I_ACC_NO_SAFEPOINTS);
    }

    @Override
    public boolean isNoReturn() {
        return getExecutableElement().hasNoModifiersOf(ClassFile.I_ACC_NO_RETURN);
    }

    @Override
    public boolean isNoSideEffect() {
        return getExecutableElement().hasNoModifiersOf(ClassFile.I_ACC_NO_SIDE_EFFECTS);
    }

    public SafePointBehavior safePointBehavior() {
        return getExecutableElement().safePointBehavior();
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
