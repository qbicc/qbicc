package cc.quarkus.qcc.type.definition.element;

/**
 *
 */
public final class ConstructorElement extends InvokableElement {
    public static final ConstructorElement[] NO_CONSTRUCTORS = new ConstructorElement[0];

    ConstructorElement(Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends InvokableElement.Builder {
        Builder() {}

        public ConstructorElement build() {
            return new ConstructorElement(this);
        }
    }
}
