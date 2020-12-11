package cc.quarkus.qcc.type.definition.element;

/**
 * A method local variable.
 */
public final class LocalVariableElement extends VariableElement {

    LocalVariableElement(final Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends VariableElement.Builder {
        public LocalVariableElement build() {
            return new LocalVariableElement(this);
        }
    }
}
