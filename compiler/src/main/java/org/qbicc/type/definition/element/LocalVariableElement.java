package org.qbicc.type.definition.element;

import org.qbicc.context.ClassContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A method local variable.
 */
public final class LocalVariableElement extends VariableElement {
    private final ParameterElement reflectsParameter;
    private final int line;
    private final int bci;

    LocalVariableElement(final BuilderImpl builder) {
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

    public ParameterElement getReflectsParameter() {
        return reflectsParameter;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    ValueType resolveTypeDescriptor(ClassContext classContext, TypeParameterContext paramCtxt) {
        if (reflectsParameter != null) {
            return classContext.resolveTypeFromMethodDescriptor(
                            getTypeDescriptor(),
                            paramCtxt,
                            getTypeSignature()
            );
        }
        return super.resolveTypeDescriptor(classContext, paramCtxt);
    }

    public static Builder builder(String name, TypeDescriptor typeDescriptor, int index) {
        return new BuilderImpl(name, typeDescriptor, index);
    }

    public interface Builder extends VariableElement.Builder {
        void setReflectsParameter(ParameterElement reflectsParameter);

        void setLine(int line);

        void setBci(int bci);

        LocalVariableElement build();

        interface Delegating extends VariableElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setReflectsParameter(ParameterElement reflectsParameter) {
                getDelegate().setReflectsParameter(reflectsParameter);
            }

            @Override
            default void setLine(int line) {
                getDelegate().setLine(line);
            }

            @Override
            default void setBci(int bci) {
                getDelegate().setBci(bci);
            }

            @Override
            default LocalVariableElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends VariableElement.BuilderImpl implements Builder {
        private ParameterElement reflectsParameter;
        private int line;
        private int bci = -1;

        BuilderImpl(String name, TypeDescriptor typeDescriptor, int index) {
            super(name, typeDescriptor, index);
        }

        public void setReflectsParameter(ParameterElement reflectsParameter) {
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
