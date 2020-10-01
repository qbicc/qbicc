package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

final class InitializedTypeDefinitionImpl implements InitializedTypeDefinition {
    private final PreparedTypeDefinitionImpl delegate;

    InitializedTypeDefinitionImpl(final PreparedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
    }

    public ClassType getClassType() {
        return delegate.getClassType();
    }

    public InitializedTypeDefinition getSuperClass() {
        return delegate.getSuperClass().initialize();
    }

    public InitializedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index).initialize();
    }

    public FieldSet getInstanceFieldSet() {
        return delegate.getInstanceFieldSet();
    }

    public FieldSet getStaticFieldSet() {
        return delegate.getStaticFieldSet();
    }

    public FieldContainer getStaticFields() {
        return delegate.getStaticFields();
    }

    public FieldElement getField(final int index) {
        return delegate.getField(index);
    }

    public MethodElement getMethod(final int index) {
        return delegate.getMethod(index);
    }

    public ConstructorElement getConstructor(final int index) {
        return delegate.getConstructor(index);
    }

    public InitializerElement getInitializer() {
        return delegate.getInitializer();
    }

    public JavaObject getDefiningClassLoader() {
        return delegate.getDefiningClassLoader();
    }

    public String getInternalName() {
        return delegate.getInternalName();
    }

    public boolean internalNameEquals(final String internalName) {
        return delegate.internalNameEquals(internalName);
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }

    public boolean hasSuperClass() {
        return delegate.hasSuperClass();
    }

    public String getSuperClassInternalName() {
        return delegate.getSuperClassInternalName();
    }

    public boolean superClassInternalNameEquals(final String internalName) {
        return delegate.superClassInternalNameEquals(internalName);
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public String getInterfaceInternalName(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterfaceInternalName(index);
    }

    public boolean interfaceInternalNameEquals(final int index, final String internalName) throws IndexOutOfBoundsException {
        return delegate.interfaceInternalNameEquals(index, internalName);
    }

    public int getFieldCount() {
        return delegate.getFieldCount();
    }

    public int getMethodCount() {
        return delegate.getMethodCount();
    }

    public int getConstructorCount() {
        return delegate.getConstructorCount();
    }

    public int getVisibleAnnotationCount() {
        return delegate.getVisibleAnnotationCount();
    }

    public Annotation getVisibleAnnotation(final int index) {
        return delegate.getVisibleAnnotation(index);
    }

    public int getInvisibleAnnotationCount() {
        return delegate.getInvisibleAnnotationCount();
    }

    public Annotation getInvisibleAnnotation(final int index) {
        return delegate.getInvisibleAnnotation(index);
    }
}
