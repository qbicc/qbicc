package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.CompilationContext;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;

/**
 * A global variable.
 */
public final class GlobalVariableElement extends VariableElement {
    private static final VarHandle pointerHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "pointer", VarHandle.class, GlobalVariableElement.class, GlobalPointer.class);
    @SuppressWarnings("unused") // pointerHandle
    private volatile GlobalPointer pointer;
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

    /**
     * Get the pointer to this global variable.  Convenience method which delegates to {@link GlobalPointer#of}.
     *
     * @return the pointer
     */
    public GlobalPointer getPointer() {
        return GlobalPointer.of(this);
    }

    /**
     * Establish the pointer for this global variable; intended only for use by {@link GlobalPointer#of}.
     *
     * @param factory the factory
     * @return the pointer
     * @see GlobalPointer#of
     */
    public GlobalPointer getOrCreatePointer(Function<GlobalVariableElement, GlobalPointer> factory) {
        GlobalPointer pointer = this.pointer;
        if (pointer == null) {
            pointer = factory.apply(this);
            GlobalPointer appearing = (GlobalPointer) pointerHandle.compareAndExchange(this, null, pointer);
            if (appearing != null) {
                pointer = appearing;
            }
        }
        return pointer;
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
            super(name, typeDescriptor, 0);
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
