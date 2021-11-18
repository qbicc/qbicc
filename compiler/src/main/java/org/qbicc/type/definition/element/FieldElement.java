package org.qbicc.type.definition.element;

import org.qbicc.graph.literal.Literal;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;

/**
 *
 */
public final class FieldElement extends VariableElement implements MemberElement {
    public static final FieldElement[] NO_FIELDS = new FieldElement[0];
    private final Literal initialValue;
    private final InitializerElement runTimeInitializer;

    FieldElement(BuilderImpl builder) {
        super(builder);
        this.initialValue = builder.initialValue;
        this.runTimeInitializer = builder.runTimeInitializer;
    }

    public String toString() {
        final ClassTypeDescriptor desc = getEnclosingType().getDescriptor();
        final String packageName = desc.getPackageName();
        if (packageName.isEmpty()) {
            return desc.getClassName()+"."+getName();
        }
        return packageName + "." + desc.getClassName() + "." + getName();
    }

    public boolean isVolatile() {
        int masked = getModifiers() & (ClassFile.ACC_VOLATILE | ClassFile.ACC_FINAL | ClassFile.I_ACC_NOT_REALLY_FINAL);
        return masked == ClassFile.ACC_VOLATILE || masked == (ClassFile.ACC_FINAL | ClassFile.I_ACC_NOT_REALLY_FINAL);
    }

    public boolean isReallyFinal() {
        return (getModifiers() & (ClassFile.ACC_FINAL | ClassFile.I_ACC_NOT_REALLY_FINAL)) == ClassFile.ACC_FINAL;
    }

    public Literal getInitialValue() {
        return initialValue;
    }

    public InitializerElement getRunTimeInitializer() {
        return runTimeInitializer;
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isThreadLocal() {
        return hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
    }

    public interface Builder extends VariableElement.Builder, MemberElement.Builder {
        void setInitialValue(final Literal initialValue);

        void setRunTimeInitializer(InitializerElement runTimeInitializer);

        FieldElement build();

        interface Delegating extends VariableElement.Builder.Delegating, MemberElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default void setInitialValue(final Literal initialValue) {
                getDelegate().setInitialValue(initialValue);
            }

            @Override
            default void setRunTimeInitializer(InitializerElement runTimeInitializer) {
                getDelegate().setRunTimeInitializer(runTimeInitializer);
            }

            @Override
            default FieldElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends VariableElement.BuilderImpl implements Builder {
        BuilderImpl() {}

        private Literal initialValue;
        private InitializerElement runTimeInitializer;

        public void setInitialValue(final Literal initialValue) {
            this.initialValue = initialValue;
        }

        public void setRunTimeInitializer(InitializerElement runTimeInitializer) {
            this.runTimeInitializer = runTimeInitializer;
        }

        public FieldElement build() {
            if ((modifiers & ClassFile.ACC_STATIC) != 0) {
                setTypeParameterContext(TypeParameterContext.EMPTY);
            } else {
                setTypeParameterContext(enclosingType);
            }
            return new FieldElement(this);
        }
    }
}
