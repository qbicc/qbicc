package org.qbicc.type.definition.element;

import org.qbicc.context.ClassContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method local variable.
 */
public final class LocalVariableElement extends VariableElement {
    private final boolean reflectsParameter;
    private final int line;
    private final int bci;

    LocalVariableElement(final Builder builder) {
        super(builder);
        reflectsParameter = builder.reflectsParameter;
        line = builder.line;
        bci = builder.bci;
    }

    public int getLine() {
        return line;
    }

    public int getBci() {
        return bci;
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
        private int line;
        private int bci = -1;

        public void setReflectsParameter(boolean reflectsParameter) {
            this.reflectsParameter = reflectsParameter;
        }

        public void setLine(int line) {
            this.line = line;
        }

        public void setBci(int bci) {
            this.bci = bci;
        }

        public LocalVariableElement build() {
            return new LocalVariableElement(this);
        }
    }
}
