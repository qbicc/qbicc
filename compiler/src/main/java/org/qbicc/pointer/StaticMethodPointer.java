package org.qbicc.pointer;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.StaticMethodElement;

/**
 * A pointer to a static method.
 */
public final class StaticMethodPointer extends RootPointer implements ExecutableElementPointer {
    private final StaticMethodElement staticMethod;

    StaticMethodPointer(StaticMethodElement staticMethod) {
        super(staticMethod.getType().getPointer());
        this.staticMethod = staticMethod;
    }

    public static StaticMethodPointer of(final StaticMethodElement methodElement) {
        return methodElement.getOrCreateStaticMethodPointer(StaticMethodPointer::new);
    }

    @Deprecated
    public StaticMethodElement getStaticMethod() {
        return getExecutableElement();
    }

    @Override
    public StaticMethodElement getExecutableElement() {
        return staticMethod;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + staticMethod.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof StaticMethodPointer smp && equals(smp);
    }

    public boolean equals(final StaticMethodPointer other) {
        return super.equals(other) && staticMethod == other.staticMethod;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(staticMethod.getEnclosingType().getInternalName()).append('#').append(staticMethod.getName()).append(staticMethod.getDescriptor());
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
