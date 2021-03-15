package cc.quarkus.qcc.type.definition.element;

import cc.quarkus.qcc.type.generic.TypeParameterContext;

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
        Builder() {
            setTypeParameterContext(TypeParameterContext.EMPTY);
        }

        public GlobalVariableElement build() {
            return new GlobalVariableElement(this);
        }
    }
}
