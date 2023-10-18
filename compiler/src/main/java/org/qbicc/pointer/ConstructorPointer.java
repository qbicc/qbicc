package org.qbicc.pointer;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;

/**
 * An exact pointer to a constructor.
 */
public final class ConstructorPointer extends RootPointer implements ExecutableElementPointer {
    private final ConstructorElement constructor;

    ConstructorPointer(ConstructorElement constructor) {
        super(constructor.getType().getPointer());
        this.constructor = constructor;
    }

    public static ConstructorPointer of(final ConstructorElement methodElement) {
        return methodElement.getOrCreatePointer(ConstructorPointer::new);
    }

    public ConstructorElement getExecutableElement() {
        return constructor;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + constructor.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof ConstructorPointer imp && equals(imp);
    }

    public boolean equals(final ConstructorPointer other) {
        return this == other || super.equals(other) && constructor == other.constructor;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(constructor.getEnclosingType().getInternalName()).append('#').append("<init>");
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
