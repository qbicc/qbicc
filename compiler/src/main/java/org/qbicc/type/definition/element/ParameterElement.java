package org.qbicc.type.definition.element;

import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method parameter variable.
 */
public final class ParameterElement extends VariableElement {
    public static final ParameterElement[] NO_PARAMETERS = new ParameterElement[0];

    ParameterElement(final Builder builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(ParameterElement original) {
        return new Builder(original);
    }

    ValueType resolveTypeDescriptor(final ClassContext classContext, TypeParameterContext paramCtxt) {
        return classContext.resolveTypeFromMethodDescriptor(
                        getTypeDescriptor(),
                        paramCtxt,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public static final class Builder extends VariableElement.Builder {
        Builder() {}

        Builder(ParameterElement original) {
            super(original);
        }

        public ParameterElement build() {
            return new ParameterElement(this);
        }
    }
}
