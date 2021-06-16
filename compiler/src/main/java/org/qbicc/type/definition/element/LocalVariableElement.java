package org.qbicc.type.definition.element;

import org.qbicc.context.ClassContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method local variable.
 */
public final class LocalVariableElement extends VariableElement {
    private final boolean reflectsParameter;

    LocalVariableElement(final Builder builder) {
        super(builder);
        reflectsParameter = builder.reflectsParameter;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    ValueType resolveTypeDescriptor(ClassContext classContext, TypeParameterContext paramCtxt) {
        if (reflectsParameter) {
            return classContext.resolveTypeFromMethodDescriptor(
                            getTypeDescriptor(),
                            paramCtxt,
                            getTypeSignature(),
                            getVisibleTypeAnnotations(),
                            getInvisibleTypeAnnotations());
        }
        return super.resolveTypeDescriptor(classContext, paramCtxt);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends VariableElement.Builder {
        private boolean reflectsParameter;

        public void setReflectsParameter(boolean reflectsParameter) {
            this.reflectsParameter = reflectsParameter;
        }

        public LocalVariableElement build() {
            return new LocalVariableElement(this);
        }
    }
}
