package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.InterfaceType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaClass;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

/**
 *
 */
final class VerifiedTypeDefinitionImpl implements VerifiedTypeDefinition {
    private final DefinedTypeDefinitionImpl delegate;
    private final JavaClass javaClass;
    private final ClassType classType;
    private final VerifiedTypeDefinition superType;
    private final VerifiedTypeDefinition[] interfaces;
    private final FieldElement[] fields;
    private final MethodElement[] methods;
    private final ConstructorElement[] ctors;
    private final InitializerElement init;
    private final FieldSet staticFieldSet;
    private final FieldSet instanceFieldSet;
    private volatile ResolvedTypeDefinition resolved;

    VerifiedTypeDefinitionImpl(final DefinedTypeDefinitionImpl delegate, final VerifiedTypeDefinition superType, final VerifiedTypeDefinition[] interfaces, final FieldElement[] fields, final MethodElement[] methods, final ConstructorElement[] ctors, final InitializerElement init) {
        this.delegate = delegate;
        this.superType = superType;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;
        this.ctors = ctors;
        this.init = init;
        int interfaceCnt = interfaces.length;
        InterfaceType[] interfaceTypes = new InterfaceType[interfaceCnt];
        for (int i = 0; i < interfaceCnt; i ++) {
            ClassType classType = interfaces[i].getClassType();
            if (! (classType instanceof InterfaceType)) {
                throw new VerifyFailedException("Type " + classType.getClassName() + " is not an interface");
            }
            interfaceTypes[i] = (InterfaceType) classType;
        }
        if (isInterface()) {
            classType = Type.interfaceType(this, interfaceTypes);
        } else {
            classType = Type.classType(this, superType == null ? null : superType.getClassType(), interfaceTypes);
        }
        instanceFieldSet = new FieldSet(this, false);
        staticFieldSet = new FieldSet(this, true);
        javaClass = null; // TODO: chicken/egg situation
    }

    // delegates

    public String getInternalName() {
        return delegate.getInternalName();
    }

    public boolean internalNameEquals(final String internalName) {
        return delegate.internalNameEquals(internalName);
    }

    public int getModifiers() {
        return delegate.getModifiers();
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

    public JavaObject getDefiningClassLoader() {
        return delegate.getDefiningClassLoader();
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

    public boolean hasSuperClass() {
        return delegate.hasSuperClass();
    }

    // local methods

    public ClassType getClassType() {
        return classType;
    }

    public VerifiedTypeDefinition getSuperClass() {
        return superType;
    }

    public VerifiedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
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
        VerifiedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.resolve();
        }
        int cnt = getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            getInterface(i).resolve();
        }
        cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            fields[i].getType();
        }
        cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = methods[i];
            method.getReturnType();
            int cnt2 = method.getParameterCount();
            for (int j = 0; j < cnt2; j ++) {
                method.getParameter(j).getType();
            }
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

