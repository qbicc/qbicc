package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.graph.ArrayType;
import cc.quarkus.qcc.graph.ClassType;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class ResolvedTypeDefinitionImpl implements ResolvedTypeDefinition {
    private final VerifiedTypeDefinition delegate;
    private volatile PreparedTypeDefinitionImpl prepared;

    ResolvedTypeDefinitionImpl(final VerifiedTypeDefinition delegate) {
        this.delegate = delegate;
    }

    // delegation

    public ClassType getClassType() {
        return delegate.getClassType();
    }

    public ResolvedTypeDefinition getSuperClass() {
        VerifiedTypeDefinition superClass = delegate.getSuperClass();
        return superClass == null ? null : superClass.resolve();
    }

    public ResolvedTypeDefinition getInterface(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterface(index).resolve();
    }

    public boolean isArray() {
        return delegate.isArray();
    }

    public Universe getDefiningClassLoader() {
        return delegate.getDefiningClassLoader();
    }

    public String getName() {
        return delegate.getName();
    }

    public int getModifiers() {
        return delegate.getModifiers();
    }

    public String getSuperClassName() {
        return delegate.getSuperClassName();
    }

    public int getInterfaceCount() {
        return delegate.getInterfaceCount();
    }

    public String getInterfaceName(final int index) throws IndexOutOfBoundsException {
        return delegate.getInterfaceName(index);
    }

    public int getFieldCount() {
        return delegate.getFieldCount();
    }

    public int getMethodCount() {
        return delegate.getMethodCount();
    }

    public ResolvedFieldDefinition getFieldDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getFieldDefinition(index).resolve();
    }

    public ResolvedMethodDefinition getMethodDefinition(final int index) throws IndexOutOfBoundsException {
        return delegate.getMethodDefinition(index).resolve();
    }

    // next phase

    public PreparedTypeDefinitionImpl prepare() throws PrepareFailedException {
        PreparedTypeDefinitionImpl prepared = this.prepared;
        if (prepared != null) {
            return prepared;
        }
        ResolvedTypeDefinition superClass = getSuperClass();
        if (superClass != null) {
            superClass.prepare();
        }
        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            getInterface(i).prepare();
        }
        synchronized (this) {
            prepared = this.prepared;
            if (prepared == null) {
                prepared = new PreparedTypeDefinitionImpl(this);
                getDefiningClassLoader().replaceTypeDefinition(getName(), this, prepared);
                this.prepared = prepared;
            }
        }
        return prepared;
    }

    // fields

    @Override
    public ResolvedFieldDefinition resolveField(final Type type, final String name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        // JVMS 5.4.3.2. Field Resolution

        // 1. If C declares a field with the name and descriptor specified by the field reference,
        // field lookup succeeds. The declared field is the result of the field lookup.

        int fieldCount = getFieldCount();
        for (int i = 0; i < fieldCount; i ++) {
            ResolvedFieldDefinition field = getFieldDefinition(i);
            if ( field.getName().equals(name) && field.getType() == type ) {
                return field;
            }
        }

        // 2. Otherwise, field lookup is applied recursively to the direct superinterfaces
        // of the specified class or interface C.

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            ResolvedTypeDefinition each = getInterface(i);
            ResolvedFieldDefinition candidate = each.resolveField(type, name);
            if ( candidate != null ) {
                return candidate;
            }
        }

        // 3. Otherwise, if C has a superclass S, field lookup is applied recursively to S.

        ResolvedTypeDefinition superType = getSuperClass();
        return superType != null ? superType.resolveField(type, name) : null;
    }

    public ResolvedFieldDefinition findField(final String name) {
        int fieldCount = getFieldCount();
        for (int i = 0; i < fieldCount; i ++) {
            ResolvedFieldDefinition field = getFieldDefinition(i);
            if ( field.getName().equals(name)) {
                return field;
            }
        }

        throw new LinkageException("Unresolved field " + name);
    }

    // methods

    @Override
    public ResolvedMethodDefinition resolveMethod(MethodIdentifier identifier) {
        // JVMS 5.4.4.3

        // 1. If C is an interface, method resolution throws an IncompatibleClassChangeError.
        if (isInterface()) {
            throw new IncompatibleClassChangeError(getName() + " is an interface");
        }

        int methodCnt = getMethodCount();

        // 2. Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:

        // 2.a If C declares exactly one method with the name specified by the method
        // reference, and the declaration is a signature polymorphic method (§2.9.3),
        // then method lookup succeeds. All the class names mentioned in the descriptor
        // are resolved (§5.4.3.1).
        //
        // The resolved method is the signature polymorphic method declaration. It is
        // not necessary for C to declare a method with the descriptor specified by the method reference.
        if ( isMethodHandleOrVarHandle() ) {
            ResolvedMethodDefinition candidate = null;
            for (int i = 0; i < methodCnt; i ++) {
                if (! getMethodDefinition(i).getName().equals(identifier.getName())) {
                    if (candidate != null) {
                        candidate = null; // two or more
                        break;
                    }
                    candidate = getMethodDefinition(i);
                }
            }
            if (candidate != null) {
                if ( candidate.isVarArgs() && candidate.isNative() ) {
                    if ( candidate.getParameterCount() == 1 ) {
                        Type theType = candidate.getParameterType(0);
                        if (theType instanceof ArrayType) {
                            Type elementType = ((ArrayType) theType).getElementType();
                            if (elementType instanceof ClassType) {
                                if (((ClassType) elementType).getClassName().equals("java/lang/Object")) {
                                    return candidate;
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2.b Otherwise, if C declares a method with the name and descriptor specified by
        // the method reference, method lookup succeeds.
        for (int i = 0; i < methodCnt; i ++) {
            ResolvedMethodDefinition methodDefinition = getMethodDefinition(i);
            if (methodMatches(identifier, methodDefinition)) {
                return methodDefinition;
            }
        }

        // 2.c Otherwise, if C has a superclass, step 2 of method resolution is recursively
        // invoked on the direct superclass of C.

        if ( getSuperClass() != null ) {
            ResolvedMethodDefinition superCandidate = getSuperClass().resolveMethod(identifier);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            ResolvedTypeDefinition each = getInterface(i);
            ResolvedMethodDefinition interfaceCandidate = each.resolveInterfaceMethod(identifier);
            if (interfaceCandidate != null) {
                return interfaceCandidate;
            }
        }

        return null;
    }

    boolean methodMatches(final MethodIdentifier methodDescriptor, final ResolvedMethodDefinition methodDefinition) {
        if (methodDefinition.getName().equals(methodDescriptor.getName())) {
            if (methodDefinition.getReturnType().equals(methodDescriptor.getReturnType())) {
                int parameterCount = methodDefinition.getParameterCount();
                if (methodDescriptor.getParameterCount() == parameterCount) {
                    for (int j = 0; j < parameterCount; j ++) {
                        if (! methodDefinition.getParameterType(j).equals(methodDescriptor.getParameterType(j))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    public ResolvedMethodDefinition resolveInterfaceMethod(MethodIdentifier identifier) {
        return resolveInterfaceMethod(identifier, false);
    }

    public ResolvedMethodDefinition resolveInterfaceMethod(MethodIdentifier identifier, boolean searchingSuper) {
        // 5.4.3.4. Interface Method Resolution

        // 1. If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
        if (!isInterface()) {
            throw new IncompatibleClassChangeError(getName() + " is not an interface");
        }

        // 2. Otherwise, if C declares a method with the name and descriptor specified
        // by the interface method reference, method lookup succeeds.

        int methodCount = getMethodCount();
        for (int i = 0; i < methodCount; i ++) {
            ResolvedMethodDefinition candidate = getMethodDefinition(i);
            if (methodMatches(identifier, candidate)) {
                if (! searchingSuper) {
                    return candidate;
                }

                if (!candidate.isPrivate() && !candidate.isStatic()) {
                    return candidate;
                }
                break;
            }
        }

        // 3. Otherwise, if the class Object declares a method with the name and descriptor
        // specified by the interface method reference, which has its ACC_PUBLIC flag set
        // and does not have its ACC_STATIC flag set, method lookup succeeds.

        ResolvedMethodDefinition objectCandidate = getDefiningClassLoader().findClass("java/lang/Object").verify().resolve().resolveMethod(identifier);
        if ( objectCandidate != null ) {
            if ( objectCandidate.isPublic() && ! objectCandidate.isStatic()) {
                return objectCandidate;
            }
        }

        // 4. Otherwise, if the maximally-specific superinterface methods (§5.4.3.3) of C
        // for the name and descriptor specified by the method reference include exactly
        // one method that does not have its ACC_ABSTRACT flag set, then this method is
        // chosen and method lookup succeeds.

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            ResolvedTypeDefinition each = getInterface(i);
            // todo: is this right?
            ResolvedMethodDefinition superCandidate = getSuperClass().resolveInterfaceMethod(identifier);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        // 5. Otherwise, if any superinterface of C declares a method with the name and descriptor
        // specified by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
        // flag set, one of these is arbitrarily chosen and method lookup succeeds.

        for (int i = 0; i < interfaceCount; i ++) {
            ResolvedTypeDefinition each = getInterface(i);
            ResolvedMethodDefinition interfaceCandidate = each.resolveInterfaceMethod(identifier, true);
            if ( interfaceCandidate != null ) {
                return interfaceCandidate;
            }
        }

        return null;
    }

    protected boolean isMethodHandleOrVarHandle() {
        return getName().equals("java/lang/invoke/MethodHandle") || getName().equals("java/lang/invoke/VarHandle");
    }
}
