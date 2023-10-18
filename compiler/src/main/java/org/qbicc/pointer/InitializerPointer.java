package org.qbicc.pointer;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.InitializerElement;

/**
 * A pointer to a static initializer.
 */
public final class InitializerPointer extends RootPointer implements ExecutableElementPointer {
    private final InitializerElement initializerElement;

    InitializerPointer(InitializerElement initializerElement) {
        super(initializerElement.getType().getPointer());
        this.initializerElement = initializerElement;
    }

    public static InitializerPointer of(final InitializerElement initializerElement) {
        return initializerElement.getOrCreatePointer(InitializerPointer::new);
    }

    public InitializerElement getExecutableElement() {
        return initializerElement;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + initializerElement.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof InitializerPointer smp && equals(smp);
    }

    public boolean equals(final InitializerPointer other) {
        return super.equals(other) && initializerElement == other.initializerElement;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(initializerElement.getEnclosingType().getInternalName()).append("<clinit>");
    }

    public boolean isNoThrow() {
        return getExecutableElement().hasNoModifiersOf(ClassFile.I_ACC_NO_THROW);
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
