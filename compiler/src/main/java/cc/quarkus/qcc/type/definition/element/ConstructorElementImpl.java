package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;

final class ConstructorElementImpl extends AbstractParameterizedExecutableElement implements ConstructorElement {

    private final TypeResolver resolver;
    private final int argument;

    ConstructorElementImpl(final Builder builder) {
        super(builder);
        resolver = builder.resolver;
        argument = builder.argument;
    }

    public ConstructorDescriptor getDescriptor() {
        return resolver.resolveConstructorDescriptor(argument);
    }

    public String toString() {
        return "<init>:" + getEnclosingType().getInternalName();
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static final class Builder extends AbstractParameterizedExecutableElement.Builder implements ConstructorElement.Builder {
        TypeResolver resolver;
        int argument;

        public void setConstructorTypeResolver(final TypeResolver resolver, final int argument) {
            this.resolver = resolver;
            this.argument = argument;
        }

        public ConstructorElementImpl build() {
            return new ConstructorElementImpl(this);
        }
    }
}
