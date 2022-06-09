package org.qbicc.pointer;

import org.qbicc.type.definition.element.StaticMethodElement;

/**
 * A pointer to a static method.
 */
public final class StaticMethodPointer extends RootPointer {
    private final StaticMethodElement staticMethod;

    StaticMethodPointer(StaticMethodElement staticMethod) {
        super(staticMethod.getType().getPointer());
        this.staticMethod = staticMethod;
    }

    public static StaticMethodPointer of(final StaticMethodElement methodElement) {
        return methodElement.getOrCreateStaticMethodPointer(StaticMethodPointer::new);
    }

    public StaticMethodElement getStaticMethod() {
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

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
