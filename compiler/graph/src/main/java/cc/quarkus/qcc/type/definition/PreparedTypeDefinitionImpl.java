package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.interpreter.Thrown;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 *
 */
final class PreparedTypeDefinitionImpl implements PreparedTypeDefinition {
    private final ResolvedTypeDefinitionImpl delegate;
    private final FieldContainer staticFields;
    private volatile PreparedTypeDefinition initialized;
    private PreparedTypeDefinition initializing;

    PreparedTypeDefinitionImpl(final ResolvedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
        staticFields = FieldContainer.forStaticFieldsOf(delegate);
    }

    // delegations

    public ClassType getClassType() {
        return delegate.getClassType();
    }

    public PreparedTypeDefinition getSuperClass() {
        ResolvedTypeDefinition superClass = delegate.getSuperClass();
        return superClass == null ? null : superClass.prepare();
    }

    public PreparedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index).prepare();
    }

    public FieldSet getInstanceFieldSet() {
        return delegate.getInstanceFieldSet();
    }

    public FieldSet getStaticFieldSet() {
        return delegate.getStaticFieldSet();
    }

    public FieldContainer getStaticFields() {
        return staticFields;
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

    public MethodDescriptor resolveMethodDescriptor(final int argument) throws ResolutionFailedException {
        return delegate.resolveMethodDescriptor(argument);
    }

    public InitializedTypeDefinition initialize() throws InitializationFailedException {
        PreparedTypeDefinition initialized = this.initialized;
        if (initialized != null) {
            return initialized.initialize();
        }
        PreparedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.initialize();
        }
        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            getInterface(i).initialize();
        }
        synchronized (this) {
            initialized = this.initialized;
            if (initialized == null) {
                if (initializing != null) {
                    // init in progress from this same thread
                    return initializing.initialize();
                }
                this.initializing = initialized = new InitializedTypeDefinitionImpl(this);
                JavaVM vm = JavaVM.requireCurrent();
                try {
                    vm.invokeInitializer(getInitializer());
                } catch (Thrown t) {
                    InitializationFailedException ex = new InitializationFailedException(t);
                    this.initialized = new InitializationFailedDefinitionImpl(this, ex);
                    this.initializing = null;
                    throw ex;
                }
                this.initialized = initialized;
                this.initializing = null;
            }
        }
        return initialized.initialize();
    }
}
