package cc.quarkus.qcc.type.definition.element;

final class InitializerElementImpl extends AbstractExactExecutableElement implements InitializerElement {
    InitializerElementImpl(final Builder builder) {
        super(builder);
    }

    static final class Builder extends AbstractExactExecutableElement.Builder implements InitializerElement.Builder {
        public InitializerElementImpl build() {
            return new InitializerElementImpl(this);
        }
    }
}
