package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.annotation.Annotation;

/**
 *
 */
public abstract class DelegatingDefinedTypeDefinition implements DefinedTypeDefinition {
    protected DelegatingDefinedTypeDefinition() {}

    protected abstract DefinedTypeDefinition getDelegate();

    public VerifiedTypeDefinition verify() throws VerifyFailedException {
        return getDelegate().verify();
    }

    public JavaObject getDefiningClassLoader() {
        return getDelegate().getDefiningClassLoader();
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
}
