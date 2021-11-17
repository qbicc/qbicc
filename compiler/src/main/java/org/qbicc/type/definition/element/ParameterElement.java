package org.qbicc.type.definition.element;

import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method parameter variable.
 */
public final class ParameterElement extends VariableElement {
    public static final ParameterElement[] NO_PARAMETERS = new ParameterElement[0];

    ParameterElement(final BuilderImpl builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public static Builder builder(ParameterElement original) {
        return new BuilderImpl(original);
    }

    ValueType resolveTypeDescriptor(final ClassContext classContext, TypeParameterContext paramCtxt) {
        return classContext.resolveTypeFromMethodDescriptor(
                        getTypeDescriptor(),
                        paramCtxt,
                        getTypeSignature(),
                        getVisibleTypeAnnotations(),
                        getInvisibleTypeAnnotations());
    }

    public interface Builder extends VariableElement.Builder {
        ParameterElement build();
    }

    static final class BuilderImpl extends VariableElement.BuilderImpl implements Builder {
        BuilderImpl() {}

        BuilderImpl(ParameterElement original) {
            super(original);
        }

        public ParameterElement build() {
            return new ParameterElement(this);
        }
    }
}
