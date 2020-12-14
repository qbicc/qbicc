package cc.quarkus.qcc.type.definition.element;

import java.util.List;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;

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

    ValueType resolveTypeDescriptor(final ClassContext classContext, final List<ParameterizedSignature> signatureContext) {
        return classContext.resolveTypeFromMethodDescriptor(
                        getTypeDescriptor(),
                        signatureContext,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public static final class Builder extends VariableElement.Builder {
        public ParameterElement build() {
            return new ParameterElement(this);
        }
    }
}
