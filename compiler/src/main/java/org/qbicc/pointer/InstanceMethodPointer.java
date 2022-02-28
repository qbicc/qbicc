package org.qbicc.pointer;

import org.qbicc.type.definition.element.MethodElement;

/**
 * An exact pointer to an instance method.
 */
public final class InstanceMethodPointer extends RootPointer {
    private final MethodElement instanceMethod;

    InstanceMethodPointer(MethodElement instanceMethod) {
        super(instanceMethod.getType().getPointer());
        if (instanceMethod.isStatic()) {
            throw new IllegalArgumentException("Method is static");
        }
        this.instanceMethod = instanceMethod;
    }

    public static InstanceMethodPointer of(final MethodElement methodElement) {
        return methodElement.getOrCreateInstanceMethodPointer(InstanceMethodPointer::new);
    }

    public MethodElement getInstanceMethod() {
        return instanceMethod;
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
