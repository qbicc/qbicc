package cc.quarkus.qcc.type.definition;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.type.InterfaceObjectType;
import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.NestedClassElement;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ValidatedTypeDefinitionImpl extends DelegatingDefinedTypeDefinition implements ValidatedTypeDefinition {
    private final ObjectType type;
    private final DefinedTypeDefinitionImpl delegate;
    private final ValidatedTypeDefinition superType;
    private final ValidatedTypeDefinition[] interfaces;
    private final ArrayList<FieldElement> fields;
    private final MethodElement[] methods;
    private final ConstructorElement[] ctors;
    private final InitializerElement init;
    private final FieldSet staticFieldSet;
    private final FieldSet instanceFieldSet;
    private final NestedClassElement enclosingClass;
    private final NestedClassElement[] enclosedClasses;
    private volatile ResolvedTypeDefinition resolved;
    private int typeId = -1;
    private int maximumSubtypeId = -1;

    ValidatedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final ValidatedTypeDefinition superType, final ValidatedTypeDefinition[] interfaces, final ArrayList<FieldElement> fields, final MethodElement[] methods, final ConstructorElement[] ctors, final InitializerElement init, final NestedClassElement enclosingClass, final NestedClassElement[] enclosedClasses) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.ctors = ctors;
        this.init = init;
        this.enclosingClass = enclosingClass;
        this.enclosedClasses = enclosedClasses;
        int interfaceCnt = interfaces.length;
        InterfaceObjectType[] interfaceTypes = new InterfaceObjectType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            interfaceTypes[i] = interfaces[i].getInterfaceType();
        }
        if (isInterface()) {
            type = getContext().getTypeSystem().generateInterfaceObjectType(delegate, List.of(interfaceTypes));
        } else {
            type = getContext().getTypeSystem().generateClassObjectType(delegate, superType == null ? null : superType.getClassType(), List.of(interfaceTypes));
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

    public ObjectType getType() {
        return type;
    }

    public ValidatedTypeDefinition getSuperClass() {
        return superType;
    }

    public ValidatedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return interfaces[index];
    }

    public ValidatedTypeDefinition[] getInterfaces() {
        return interfaces.clone();
    }

    public FieldSet getStaticFieldSet() {
        return staticFieldSet;
    }

    public NestedClassElement getEnclosingNestedClass() {
        return enclosingClass;
    }

    public int getEnclosedNestedClassCount() {
        return enclosedClasses.length;
    }

    public NestedClassElement getEnclosedNestedClass(final int index) throws IndexOutOfBoundsException {
        return enclosedClasses[index];
    }

    public FieldSet getInstanceFieldSet() {
        return instanceFieldSet;
    }

    public int getFieldCount() {
        return fields.size();
    }

    public FieldElement getField(final int index) {
        return fields.get(index);
    }

    public void injectField(final FieldElement field) {
        Assert.checkNotNullParam("field", field);
        if ((field.getModifiers() & ClassFile.I_ACC_HIDDEN) == 0) {
            throw new IllegalArgumentException("Injected fields must be hidden");
        }
        fields.add(field);
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
            fields.get(i).getType(List.of());
        }
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = methods[i];
            method.getType(List.of());
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

    public int getTypeId() {
        return typeId;
    }

    public int getMaximumSubtypeId() {
        return maximumSubtypeId;
    }

    public boolean isTypeIdValid() {
        return typeId != -1;
    }

    public void assignTypeId(int myTypeId) {
        // typeId shouldn't hae already been assigned
        Assert.assertTrue(typeId == -1);
        typeId = myTypeId;
    }

    public void assignMaximumSubtypeId(int subTypeId) {
        // maximumSubtypeId shouldn't hae already been assigned
        Assert.assertTrue(maximumSubtypeId == -1);
        maximumSubtypeId = subTypeId;
    }
}

