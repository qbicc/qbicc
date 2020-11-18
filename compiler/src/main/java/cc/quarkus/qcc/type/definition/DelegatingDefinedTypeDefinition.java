package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 *
 */
public abstract class DelegatingDefinedTypeDefinition implements DefinedTypeDefinition {
    protected DelegatingDefinedTypeDefinition() {}

    protected abstract DefinedTypeDefinition getDelegate();

    public ValidatedTypeDefinition validate() throws VerifyFailedException {
        return getDelegate().validate();
    }

    public ClassContext getContext() {
        return getDelegate().getContext();
    }

    public String getInternalName() {
        return getDelegate().getInternalName();
    }

    public boolean internalNameEquals(final String internalName) {
        return getDelegate().internalNameEquals(internalName);
    }

    public int getModifiers() {
        return getDelegate().getModifiers();
    }

    public boolean hasSuperClass() {
        return getDelegate().hasSuperClass();
    }

    public String getSuperClassInternalName() {
        return getDelegate().getSuperClassInternalName();
    }

    public boolean superClassInternalNameEquals(final String internalName) {
        return getDelegate().superClassInternalNameEquals(internalName);
    }

    public int getInterfaceCount() {
        return getDelegate().getInterfaceCount();
    }

    public String getInterfaceInternalName(final int index) throws IndexOutOfBoundsException {
        return getDelegate().getInterfaceInternalName(index);
    }

    public boolean interfaceInternalNameEquals(final int index, final String internalName) throws IndexOutOfBoundsException {
        return getDelegate().interfaceInternalNameEquals(index, internalName);
    }

    public int getFieldCount() {
        return getDelegate().getFieldCount();
    }

    public int getMethodCount() {
        return getDelegate().getMethodCount();
    }

    public int getConstructorCount() {
        return getDelegate().getConstructorCount();
    }

    public int getVisibleAnnotationCount() {
        return getDelegate().getVisibleAnnotationCount();
    }

    public Annotation getVisibleAnnotation(final int index) {
        return getDelegate().getVisibleAnnotation(index);
    }

    public int getInvisibleAnnotationCount() {
        return getDelegate().getInvisibleAnnotationCount();
    }

    public Annotation getInvisibleAnnotation(final int index) {
        return getDelegate().getInvisibleAnnotation(index);
    }

    public MethodDescriptor resolveMethodDescriptor(final int argument) throws ResolutionFailedException {
        return getDelegate().resolveMethodDescriptor(argument);
    }
}
