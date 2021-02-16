package cc.quarkus.qcc.type.definition.element;

/**
 * A global variable.
 */
public final class GlobalVariableElement extends VariableElement {

    GlobalVariableElement(final Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends VariableElement.Builder {
        public GlobalVariableElement build() {
            return new GlobalVariableElement(this);
        }
    }
}
