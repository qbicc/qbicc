package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.NestedClassElement;

/**
 *
 */
public abstract class DelegatingValidatedTypeDefinition extends DelegatingDefinedTypeDefinition implements ValidatedTypeDefinition {
    protected DelegatingValidatedTypeDefinition() {}

    protected abstract ValidatedTypeDefinition getDelegate();

    public TypeIdLiteral getTypeId() {
        return getDelegate().getTypeId();
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

    public ResolvedTypeDefinition resolve() throws ResolutionFailedException {
        return getDelegate().resolve();
    }

    public FieldElement getField(final int index) {
        return getDelegate().getField(index);
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
