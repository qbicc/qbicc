package org.qbicc.type.definition.element;

import org.qbicc.graph.literal.Literal;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.generic.TypeParameterContext;

/**
 *
 */
public final class FieldElement extends VariableElement implements MemberElement {
    public static final FieldElement[] NO_FIELDS = new FieldElement[0];
    private final Literal initialValue;

    FieldElement(Builder builder) {
        super(builder);
        this.initialValue = builder.initialValue;
    }

    public String toString() {
        final String packageName = getEnclosingType().getDescriptor().getPackageName();
        if (packageName.isEmpty()) {
            return getEnclosingType().getDescriptor().getClassName()+"."+getName();
        }
        return packageName+"."+getEnclosingType().getDescriptor().getClassName()+"."+getName();
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

    public static Builder builder() {
        return new Builder();
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public boolean isThreadLocal() {
        return hasAllModifiersOf(ClassFile.I_ACC_THREAD_LOCAL);
    }

    public static final class Builder extends VariableElement.Builder implements MemberElement.Builder {
        Builder() {}

        private Literal initialValue;

        public void setInitialValue(final Literal initialValue) {
            this.initialValue = initialValue;
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
