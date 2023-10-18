package org.qbicc.pointer;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.InstanceMethodElement;

/**
 * An exact pointer to an instance method.
 */
public final class InstanceMethodPointer extends RootPointer implements ExecutableElementPointer {
    private final InstanceMethodElement instanceMethod;

    InstanceMethodPointer(InstanceMethodElement instanceMethod) {
        super(instanceMethod.getType().getPointer());
        this.instanceMethod = instanceMethod;
    }

    public static InstanceMethodPointer of(final InstanceMethodElement methodElement) {
        return methodElement.getOrCreateInstanceMethodPointer(InstanceMethodPointer::new);
    }

    @Deprecated
    public InstanceMethodElement getInstanceMethod() {
        return getExecutableElement();
    }

    public InstanceMethodElement getExecutableElement() {
        return instanceMethod;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + instanceMethod.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof InstanceMethodPointer imp && equals(imp);
    }

    public boolean equals(final InstanceMethodPointer other) {
        return this == other || super.equals(other) && instanceMethod == other.instanceMethod;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(instanceMethod);
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
