package org.qbicc.type.definition.element;

import java.util.function.Function;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.literal.Literal;
import org.qbicc.type.BooleanType;
import org.qbicc.type.FloatType;
import org.qbicc.type.IntegerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.BooleanAnnotationValue;
import org.qbicc.type.annotation.DoubleAnnotationValue;
import org.qbicc.type.annotation.LongAnnotationValue;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;

/**
 *
 */
public final class FieldElement extends VariableElement implements MemberElement {
    public static final FieldElement[] NO_FIELDS = new FieldElement[0];
    private final Literal initialValue;
    private final InitializerElement runTimeInitializer;
    private final Function<FieldElement, ValueType> typeResolver;

    FieldElement(BuilderImpl builder) {
        super(builder);
        this.initialValue = builder.initialValue;
        this.runTimeInitializer = builder.runTimeInitializer;
        this.typeResolver = builder.typeResolver;
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
        return runTimeInitializer == null && (getModifiers() & (ClassFile.ACC_FINAL | ClassFile.I_ACC_NOT_REALLY_FINAL)) == ClassFile.ACC_FINAL;
    }

    public Literal getInitialValue() {
        return initialValue;
    }

    public Literal getReplacementValue(CompilationContext ctxt) {
        ValueType contentsType = getType();
        for (Annotation annotation : getInvisibleAnnotations()) {
            if (annotation.getDescriptor().packageAndClassNameEquals("org/qbicc/runtime", "SerializeAsZero")) {
                return ctxt.getLiteralFactory().zeroInitializerLiteralOfType(contentsType);
            } else if (annotation.getDescriptor().packageAndClassNameEquals("org/qbicc/runtime", "SerializeBooleanAs")) {
                if (contentsType instanceof BooleanType) {
                    return ctxt.getLiteralFactory().literalOf(((BooleanAnnotationValue) annotation.getValue("value")).booleanValue());
                } else {
                    ctxt.error("SerializeBooleanAs annotation on field of type" + contentsType);
                }
            } else if (annotation.getDescriptor().packageAndClassNameEquals("org/qbicc/runtime", "SerializeIntegralAs")) {
                if (contentsType instanceof IntegerType it) {
                    return ctxt.getLiteralFactory().literalOf(it, ((LongAnnotationValue) annotation.getValue("value")).longValue());
                } else {
                    ctxt.error("SerializeIntegralAs annotation on field of type" + contentsType);
                }
            } else if (annotation.getDescriptor().packageAndClassNameEquals("org/qbicc/runtime", "SerializeFloatingPointAs")) {
                if (contentsType instanceof FloatType ft) {
                    return ctxt.getLiteralFactory().literalOf(ft, ((DoubleAnnotationValue) annotation.getValue("value")).doubleValue());
                } else {
                    ctxt.error("SerializeFloatingPointAs annotation on field of type" + contentsType);
                }
            }
        }
        return null;
    }

    public InitializerElement getRunTimeInitializer() {
        return runTimeInitializer;
    }

    @Override
    ValueType resolveTypeDescriptor(ClassContext classContext, TypeParameterContext paramCtxt) {
        if (typeResolver != null) {
            return typeResolver.apply(this);
        }
        return super.resolveTypeDescriptor(classContext, paramCtxt);
    }

    public static Builder builder(String name, TypeDescriptor descriptor, int index) {
        return new BuilderImpl(name, descriptor, index);
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

        void setTypeResolver(Function<FieldElement, ValueType> resolver);

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
            default void setTypeResolver(Function<FieldElement, ValueType> resolver) {
                getDelegate().setTypeResolver(resolver);
            }

            @Override
            default FieldElement build() {
                return getDelegate().build();
            }
        }
    }

    static final class BuilderImpl extends VariableElement.BuilderImpl implements Builder {
        BuilderImpl(String name, TypeDescriptor typeDescriptor, int index) {
            super(name, typeDescriptor, index);
        }

        private Literal initialValue;
        private InitializerElement runTimeInitializer;
        private Function<FieldElement, ValueType> typeResolver;

        public void setInitialValue(final Literal initialValue) {
            this.initialValue = initialValue;
        }

        public void setRunTimeInitializer(InitializerElement runTimeInitializer) {
            this.runTimeInitializer = runTimeInitializer;
        }

        public void setTypeResolver(Function<FieldElement, ValueType> typeResolver) {
            this.typeResolver = typeResolver;
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
