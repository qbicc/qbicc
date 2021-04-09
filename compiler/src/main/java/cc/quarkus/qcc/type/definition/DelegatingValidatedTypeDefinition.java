package org.qbicc.type.definition;

import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;

/**
 *
 */
public abstract class DelegatingValidatedTypeDefinition extends DelegatingDefinedTypeDefinition implements ValidatedTypeDefinition {
    protected DelegatingValidatedTypeDefinition() {}

    protected abstract ValidatedTypeDefinition getDelegate();

    public ObjectType getType() {
        return getDelegate().getType();
    }

    public ClassContext getContext() {
        return getDelegate().getContext();
    }

    public ValidatedTypeDefinition getSuperClass() {
        return getDelegate().getSuperClass();
    }

    public ValidatedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return getDelegate().getInterface(index);
    }

    public ValidatedTypeDefinition[] getInterfaces() {
        return getDelegate().getInterfaces();
    }

    public FieldSet getInstanceFieldSet() {
        return getDelegate().getInstanceFieldSet();
    }

    public FieldSet getStaticFieldSet() {
        return getDelegate().getStaticFieldSet();
    }

    public NestedClassElement getEnclosingNestedClass() {
        return getDelegate().getEnclosingNestedClass();
    }

    public int getEnclosedNestedClassCount() {
        return getDelegate().getEnclosedNestedClassCount();
    }

    public NestedClassElement getEnclosedNestedClass(final int index) throws IndexOutOfBoundsException {
        return getDelegate().getEnclosedNestedClass(index);
    }

    public ValidatedTypeDefinition validate() {
        return this;
    }

    public FieldElement getField(final int index) {
        return getDelegate().getField(index);
    }

    public void injectField(final FieldElement field) {
        getDelegate().injectField(field);
    }

    public MethodElement getMethod(final int index) {
        return getDelegate().getMethod(index);
    }

    public ConstructorElement getConstructor(final int index) {
        return getDelegate().getConstructor(index);
    }

    public InitializerElement getInitializer() {
        return getDelegate().getInitializer();
    }
}
