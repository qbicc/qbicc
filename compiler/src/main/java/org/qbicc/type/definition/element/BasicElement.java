package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.qbicc.type.definition.DefinedTypeDefinition;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public abstract class BasicElement implements Element {
    private static final VarHandle modifiersHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "modifiers", VarHandle.class, BasicElement.class, int.class);

    private final DefinedTypeDefinition enclosingType;
    private final String sourceFileName;
    private final int index;
    @SuppressWarnings("FieldMayBeFinal") // VarHandle
    private volatile int modifiers;

    BasicElement() {
        enclosingType = null;
        sourceFileName = null;
        modifiers = 0;
        index = 0;
    }

    BasicElement(BuilderImpl builder) {
        enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
        sourceFileName = builder.sourceFileName;
        modifiers = builder.modifiers;
        index = builder.index;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public int getModifiers() {
        return modifiers;
    }

    /**
     * Get the element index.
     * <ul>
     *  <li>On methods, fields, and constructors, this is the index in the <em>defined type</em>, not the class file or resolver
     *  <li>On local variables, this is the index of the var slot
     *  <li>On parameters, this is the index (zero-based) of the parameter
     * </ul>
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    public boolean hasAllModifiersOf(int mask) {
        return (getModifiers() & mask) == mask;
    }

    public boolean hasNoModifiersOf(int mask) {
        return (getModifiers() & mask) == 0;
    }

    public void setModifierFlags(int flags) {
        modifiersHandle.getAndBitwiseOr(this, flags);
    }

    public void clearModifierFlags(int flags) {
        modifiersHandle.getAndBitwiseAnd(this, ~flags);
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    interface Builder extends Element.Builder {
        void setSourceFileName(String sourceFileName);

        void setModifiers(int modifiers);

        void addModifiers(int modifiers);

        void dropModifiers(int modifiers);

        boolean hasModifiers(int modifiers);

        void setEnclosingType(DefinedTypeDefinition enclosingType);

        BasicElement build();

        interface Delegating extends Element.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setSourceFileName(String sourceFileName) {
                getDelegate().setSourceFileName(sourceFileName);
            }

            @Override
            default void addModifiers(int modifiers) {
                getDelegate().addModifiers(modifiers);
            }

            @Override
            default void dropModifiers(int modifiers) {
                getDelegate().dropModifiers(modifiers);
            }

            @Override
            default boolean hasModifiers(int modifiers) {
                return getDelegate().hasModifiers(modifiers);
            }

            @Override
            default void setModifiers(int modifiers) {
                getDelegate().setModifiers(modifiers);
            }

            @Override
            default void setEnclosingType(DefinedTypeDefinition enclosingType) {
                Element.Builder.Delegating.super.setEnclosingType(enclosingType);
            }

            @Override
            default BasicElement build() {
                return getDelegate().build();
            }
        }
    }

    static abstract class BuilderImpl implements Builder {
        final int index;
        DefinedTypeDefinition enclosingType;
        String sourceFileName;
        int modifiers;

        BuilderImpl(int index) {
            this.index = index;
        }

        BuilderImpl(final BasicElement original) {
            enclosingType = original.enclosingType;
            sourceFileName = original.sourceFileName;
            modifiers = original.modifiers;
            index = original.index;
        }

        public void setSourceFileName(final String sourceFileName) {
            this.sourceFileName = sourceFileName;
        }

        public void setModifiers(final int modifiers) {
            this.modifiers = modifiers;
        }

        public void addModifiers(final int modifiers) {
            this.modifiers |= modifiers;
        }

        @Override
        public void dropModifiers(int modifiers) {
            this.modifiers &= ~modifiers;
        }

        public boolean hasModifiers(int modifiers) {
            return (this.modifiers & modifiers) == modifiers;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public abstract BasicElement build();
    }
}
