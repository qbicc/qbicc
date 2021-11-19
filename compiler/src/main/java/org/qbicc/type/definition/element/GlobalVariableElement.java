package org.qbicc.type.definition.element;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A global variable.
 */
public final class GlobalVariableElement extends VariableElement {
    private final String section;

    GlobalVariableElement(final BuilderImpl builder) {
        super(builder);
        section = builder.section;
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String getSection() {
        return section;
    }

    public static Builder builder(String name, TypeDescriptor descriptor) {
        return new BuilderImpl(name, descriptor);
    }

    public interface Builder extends VariableElement.Builder {
        void setSection(String section);

        GlobalVariableElement build();

        interface Delegating extends VariableElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setSection(String section) {
                getDelegate().setSection(section);
            }

            @Override
            default GlobalVariableElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends VariableElement.BuilderImpl implements Builder {
        private String section = CompilationContext.IMPLICIT_SECTION_NAME;

        BuilderImpl(String name, TypeDescriptor typeDescriptor) {
            super(name, typeDescriptor);
            setTypeParameterContext(TypeParameterContext.EMPTY);
        }

        public void setSection(String section) {
            this.section = Assert.checkNotNullParam("section", section);
        }

        public GlobalVariableElement build() {
            return new GlobalVariableElement(this);
        }
    }
}
