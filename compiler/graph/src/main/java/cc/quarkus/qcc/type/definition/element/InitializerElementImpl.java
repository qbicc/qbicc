package cc.quarkus.qcc.type.definition.element;

final class InitializerElementImpl extends AbstractExactExecutableElement implements InitializerElement {
    InitializerElementImpl(final Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String toString() {
        return "initializer of " + getEnclosingType().getInternalName();
    }

    static final class Builder extends AbstractExactExecutableElement.Builder implements InitializerElement.Builder {
        public InitializerElementImpl build() {
            return new InitializerElementImpl(this);
        }
    }
}
