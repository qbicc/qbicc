package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.interpreter.Thrown;
import cc.quarkus.qcc.interpreter.Vm;

/**
 *
 */
final class PreparedTypeDefinitionImpl extends DelegatingResolvedTypeDefinition implements PreparedTypeDefinition {
    private final ResolvedTypeDefinitionImpl delegate;
    private final FieldContainer staticFields;
    private volatile PreparedTypeDefinition initialized;
    private PreparedTypeDefinition initializing;

    PreparedTypeDefinitionImpl(final ResolvedTypeDefinitionImpl delegate) {
        this.delegate = delegate;
        staticFields = FieldContainer.forStaticFieldsOf(delegate);
    }

    // delegations

    public ResolvedTypeDefinition getDelegate() {
        return delegate;
    }

    public PreparedTypeDefinition validate() {
        return this;
    }

    public PreparedTypeDefinition resolve() {
        return this;
    }

    public PreparedTypeDefinition getSuperClass() {
        return super.getSuperClass().prepare();
    }

    public PreparedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return super.getInterface(index).prepare();
    }

    public FieldContainer getStaticFields() {
        return staticFields;
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
                Vm vm = Vm.requireCurrent();
                try {
                    vm.initialize(getTypeId());
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
