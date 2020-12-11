package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
final class ValidatedTypeDefinitionImpl extends DelegatingDefinedTypeDefinition implements ValidatedTypeDefinition {
    private final TypeIdLiteral typeId;
    private final DefinedTypeDefinitionImpl delegate;
    private final ValidatedTypeDefinition superType;
    private final ValidatedTypeDefinition[] interfaces;
    private final FieldElement[] fields;
    private final MethodElement[] methods;
    private final ConstructorElement[] ctors;
    private final InitializerElement init;
    private final FieldSet staticFieldSet;
    private final FieldSet instanceFieldSet;
    private volatile ResolvedTypeDefinition resolved;

    ValidatedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final ValidatedTypeDefinition superType, final ValidatedTypeDefinition[] interfaces, final FieldElement[] fields, final MethodElement[] methods, final ConstructorElement[] ctors, final InitializerElement init) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.ctors = ctors;
        this.init = init;
        int interfaceCnt = interfaces.length;
        InterfaceTypeIdLiteral[] interfaceTypes = new InterfaceTypeIdLiteral[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            TypeIdLiteral classType = interfaces[i].getTypeId();
            if (! (classType instanceof InterfaceTypeIdLiteral)) {
                throw new VerifyFailedException("Type " + getContext().resolveDefinedTypeLiteral(classType).getInternalName() + " is not an interface");
            }
            interfaceTypes[i] = (InterfaceTypeIdLiteral) classType;
        }
        if (isInterface()) {
            InterfaceTypeIdLiteral interfaceTypeId = getContext().getLiteralFactory().literalOfInterface(getInternalName(), interfaceTypes);
            getContext().registerInterfaceLiteral(interfaceTypeId, this);
            typeId = interfaceTypeId;
        } else {
            ClassTypeIdLiteral classTypeId = getContext().getLiteralFactory().literalOfClass(getInternalName(), superType == null ? null : (ClassTypeIdLiteral) superType.getTypeId(), interfaceTypes);
            getContext().registerClassLiteral(classTypeId, this);
            typeId = classTypeId;
        }
        instanceFieldSet = new FieldSet(this, false);
        staticFieldSet = new FieldSet(this, true);
    }

    // delegates

    public DefinedTypeDefinition getDelegate() {
        return delegate;
    }

    public ValidatedTypeDefinition validate() {
        return this;
    }

    // local methods

    public TypeIdLiteral getTypeId() {
        return typeId;
    }

    public ValidatedTypeDefinition getSuperClass() {
        return superType;
    }

    public ValidatedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public FieldSet getStaticFieldSet() {
        return staticFieldSet;
    }

    public FieldSet getInstanceFieldSet() {
        return instanceFieldSet;
    }

    public FieldElement getField(final int index) {
        return fields[index];
    }

    public MethodElement getMethod(final int index) {
        return methods[index];
    }

    public ConstructorElement getConstructor(final int index) {
        return ctors[index];
    }

    public InitializerElement getInitializer() {
        return init;
    }

    // next stage

    public ResolvedTypeDefinition resolve() throws ResolutionFailedException {
        ResolvedTypeDefinition resolved = this.resolved;
        if (resolved != null) {
            return resolved;
        }
        ValidatedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.resolve();
        }
        int cnt = getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            getInterface(i).resolve();
        }
        cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            fields[i].getType(getContext(), List.of());
        }
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = methods[i];
            method.getType(getContext(), List.of());
        }
        synchronized (this) {
            resolved = this.resolved;
            if (resolved != null) {
                return resolved;
            }
            resolved = new ResolvedTypeDefinitionImpl(this);
            this.resolved = resolved;
        }
        return resolved;
    }
}

