package org.qbicc.type.definition;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.classfile.BootstrapMethod;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.ClassSignature;

/**
 *
 */
public abstract class DelegatingDefinedTypeDefinition implements DefinedTypeDefinition {
    protected DelegatingDefinedTypeDefinition() {}

    protected abstract DefinedTypeDefinition getDelegate();

    public LoadedTypeDefinition load() throws VerifyFailedException {
        return getDelegate().load();
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

    public boolean internalPackageAndNameEquals(final String intPackageName, final String className) {
        return getDelegate().internalPackageAndNameEquals(intPackageName, className);
    }

    public ClassTypeDescriptor getDescriptor() {
        return getDelegate().getDescriptor();
    }

    public int getModifiers() {
        return getDelegate().getModifiers();
    }

    public String getEnclosingClassInternalName() {
        return getDelegate().getEnclosingClassInternalName();
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

    public ClassSignature getSignature() {
        return getDelegate().getSignature();
    }

    public List<Annotation> getVisibleAnnotations() {
        return getDelegate().getVisibleAnnotations();
    }

    public List<Annotation> getInvisibleAnnotations() {
        return getDelegate().getInvisibleAnnotations();
    }

    public TypeAnnotationList getVisibleTypeAnnotations() {
        return getDelegate().getVisibleTypeAnnotations();
    }

    public TypeAnnotationList getInvisibleTypeAnnotations() {
        return getDelegate().getInvisibleTypeAnnotations();
    }

    public List<BootstrapMethod> getBootstrapMethods() { return getDelegate().getBootstrapMethods(); }

    public BootstrapMethod getBootstrapMethod(final int index) { return getDelegate().getBootstrapMethod(index); }

    public int hashCode() {
        return getDelegate().hashCode();
    }

    public boolean equals(final Object obj) {
        return getDelegate().equals(obj);
    }
}
