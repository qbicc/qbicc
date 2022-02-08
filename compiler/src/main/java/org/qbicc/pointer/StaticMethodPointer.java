package org.qbicc.pointer;

import org.qbicc.type.definition.element.MethodElement;

/**
 * A pointer to a static method.
 */
public final class StaticMethodPointer extends RootPointer {
    private final MethodElement staticMethod;

    StaticMethodPointer(MethodElement staticMethod) {
        super(staticMethod.getType().getPointer());
        if (! staticMethod.isStatic()) {
            throw new IllegalArgumentException("Method is not static");
        }
        this.staticMethod = staticMethod;
    }

    public static StaticMethodPointer of(final MethodElement methodElement) {
        return methodElement.getOrCreateStaticMethodPointer(StaticMethodPointer::new);
    }

    public MethodElement getStaticMethod() {
        return staticMethod;
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
