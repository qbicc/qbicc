package cc.quarkus.qcc.type.definition.element;

final class ConstructorElementImpl extends AbstractParameterizedExecutableElement implements ConstructorElement {
    ConstructorElementImpl(final Builder builder) {
        super(builder);
    }

    static final class Builder extends AbstractParameterizedExecutableElement.Builder implements ConstructorElement.Builder {
        public ConstructorElementImpl build() {
            return new ConstructorElementImpl(this);
        }
    }
}
