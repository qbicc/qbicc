package cc.quarkus.qcc.type.definition;

import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.ConstructorDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface ResolvedTypeDefinition extends VerifiedTypeDefinition {
    // ==================
    // Lifecycle
    // ==================

    default ResolvedTypeDefinition verify() {
        return this;
    }

    default ResolvedTypeDefinition resolve() {
        return this;
    }

    PreparedTypeDefinition prepare() throws PrepareFailedException;

    // ==================
    // Superclass
    // ==================

    ResolvedTypeDefinition getSuperClass();

//    ClassTypeSignature getSuperClassSignature() throws ResolutionFailedException;

    // ==================
    // Interfaces
    // ==================

    ResolvedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

//    ClassTypeSignature getInterfaceSignature(int index) throws IndexOutOfBoundsException, ResolutionFailedException;

    // ==================
    // Fields
    // ==================

    /**
     * Resolve a field by name and type.
     *
     * @param type the field type (must not be {@code null})
     * @param name the field name (must not be {@code null})
     * @return the field handle, or {@code null} if no matching field is found
     */
    default FieldElement resolveField(Type type, String name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        // JVMS 5.4.3.2. Field Resolution

        // 1. If C declares a field with the name and descriptor specified by the field reference,
        // field lookup succeeds. The declared field is the result of the field lookup.

        FieldElement field = getField(getFieldIndex(name));
        if ( field.getType() == type ) {
            return field;
        }

        // 2. Otherwise, field lookup is applied recursively to the direct superinterfaces
        // of the specified class or interface C.

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            ResolvedTypeDefinition each = getInterface(i);
            FieldElement candidate = each.resolveField(type, name);
            if ( candidate != null ) {
                return candidate;
            }
        }

        // 3. Otherwise, if C has a superclass S, field lookup is applied recursively to S.

        ResolvedTypeDefinition superType = getSuperClass();
        return superType != null ? superType.resolveField(type, name) : null;
    }

    /**
     * Get the index of a field, or {@code -1} if a field with the given name is not present on this type.
     *
     * @param name the field name (must not be {@code null})
     * @return the field index
     */
    default int getFieldIndex(String name) {
        int cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            if (getField(i).nameEquals(name)) {
                return i;
            }
        }
        return -1;
    }

    default FieldElement findField(String name) {
        int idx = getFieldIndex(name);
        return idx == - 1 ? null : getField(idx);
    }

    // ==================
    // Methods
    // ==================

    /**
     * Get the method index of the exactly matching method on this class.  If the method is not directly present on this class,
     * {@code -1} is returned.
     *
     * @param name       the method name (must not be {@code null})
     * @param descriptor the method descriptor (must not be {@code null})
     * @return the index of the method, or {@code -1} if it is not present on this class
     */
    default int findMethodIndex(String name, MethodDescriptor descriptor) {
        int cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = getMethod(i);
            if (method.nameEquals(name)) {
                if ((method.getModifiers() & ClassFile.I_ACC_SIGNATURE_POLYMORPHIC) != 0) {
                    return i;
                } else if (method.getDescriptor().equals(descriptor)) {
                    return i;
                }
            }
        }
        return -1;
    }

    default MethodElement resolveMethodElementExact(String name, MethodDescriptor descriptor) {
        int idx = findMethodIndex(name, descriptor);
        return idx == -1 ? null : getMethod(idx);
    }

    default MethodElement resolveMethodElementVirtual(String name, MethodDescriptor descriptor) {
        // JVMS 5.4.4.3

        // 1. If C is an interface, method resolution throws an IncompatibleClassChangeError.
        if (isInterface()) {
            // todo: remap this to exception
            throw new IncompatibleClassChangeError(getInternalName() + " is an interface");
        }

        // 2. Otherwise, method resolution attempts to locate the referenced method in C and its superclasses:

        // 2.a If C declares exactly one method with the name specified by the method
        // reference, and the declaration is a signature polymorphic method (§2.9.3),
        // then method lookup succeeds. All the class names mentioned in the descriptor
        // are resolved (§5.4.3.1).
        //
        // The resolved method is the signature polymorphic method declaration. It is
        // not necessary for C to declare a method with the descriptor specified by the method reference.
        //
        // 2.b Otherwise, if C declares a method with the name and descriptor specified by
        // the method reference, method lookup succeeds.

        int result = findMethodIndex(name, descriptor);
        if (result != -1) {
            return getMethod(result);
        }

        // 2.c Otherwise, if C has a superclass, step 2 of method resolution is recursively
        // invoked on the direct superclass of C.

        ResolvedTypeDefinition superClass = getSuperClass();
        if ( superClass != null ) {
            MethodElement superCandidate = superClass.resolveMethodElementVirtual(name, descriptor);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            MethodElement interfaceCandidate = getInterface(i).resolveMethodElementInterface(name, descriptor);
            if (interfaceCandidate != null) {
                return interfaceCandidate;
            }
        }

        // Otherwise, it's not found.

        return null;
    }

    default MethodElement resolveMethodElementInterface(String name, MethodDescriptor descriptor) {
        return resolveMethodElementInterface(false, name, descriptor);
    }

    default MethodElement resolveMethodElementInterface(boolean virtualOnly, String name, MethodDescriptor descriptor) {
        // 5.4.3.4. Interface Method Resolution

        // 1. If C is not an interface, interface method resolution throws an IncompatibleClassChangeError.
        if (!isInterface()) {
            // todo: remap this to exception
            throw new IncompatibleClassChangeError(getInternalName() + " is not an interface");
        }

        // 2. Otherwise, if C declares a method with the name and descriptor specified
        // by the interface method reference, method lookup succeeds.

        int result = findMethodIndex(name, descriptor);
        if (result != -1) {
            return getMethod(result);
        }

        // 3. Otherwise, if the class Object declares a method with the name and descriptor
        // specified by the interface method reference, which has its ACC_PUBLIC flag set
        // and does not have its ACC_STATIC flag set, method lookup succeeds.
        if (! virtualOnly) {
            ResolvedTypeDefinition object = JavaVM.currentThread().getVM().getObjectTypeDefinition().verify().resolve();
            result = object.findMethodIndex(name, descriptor);
            if (result != -1) {
                MethodElement method = object.getMethod(result);
                int modifiers = method.getModifiers();
                if ((modifiers & (ClassFile.ACC_PUBLIC | ClassFile.ACC_STATIC)) == ClassFile.ACC_PUBLIC) {
                    // it might be overridden in the implementation subclass
                    return method;
                }
            }
        }

        // 4. Otherwise, if the [set of] maximally-specific superinterface methods (§5.4.3.3) of C
        // for the name and descriptor specified by the method reference include[s] exactly
        // one method that does not have its ACC_ABSTRACT flag set, then this method is
        // chosen and method lookup succeeds.

        // Impl: To find the set of maximally-specific methods, we have to perform the dreaded breadth-first search.
        // We also do not want to maintain a set, so we need to fail fast once a second candidate is encountered.
        MethodElement candidate = resolveMaximallySpecificMethodInterface(name, descriptor);
        if (candidate != null) {
            return candidate;
        }

        // 5. Otherwise, if any superinterface of C declares a method with the name and descriptor
        // specified by the method reference that has neither its ACC_PRIVATE flag nor its ACC_STATIC
        // flag set, one of these is arbitrarily chosen and method lookup succeeds.

        // Impl: Simple depth-first search.
        int cnt = getInterfaceCount();
        for (int i = 0; i < cnt; i ++) {
            candidate = getInterface(i).resolveMethodElementInterface(true, name, descriptor);
            if ( candidate != null ) {
                return candidate;
            }
        }

        return null;
    }

    private MethodElement resolveMaximallySpecificMethodInterface(String name, MethodDescriptor descriptor) {
        MethodElement found;
        for (int d = 0; ; d ++) {
            found = resolveMaximallySpecificMethodInterface(d, name, descriptor);
            if (found == ResolvedTypeDefinitionUtil.NOT_FOUND || found == ResolvedTypeDefinitionUtil.END_OF_SEARCH) {
                return null;
            }
            if (found != null) {
                return found;
            }
            // else go deeper
        }
    }

    /**
     * Recursive step to find maximally-specific implementation methods on an interface.
     *
     * @param depth the recursion depth (how many supertype levels to search)
     * @param name the method name
     * @param descriptor the method descriptor
     * @return the handle, or {@code null} if it isn't found at this depth, or {@code NOT_FOUND} if there are conflicting
     * candidates, or {@code END_OF_SEARCH} if there are no more superinterfaces of this interface at this depth
     */
    private MethodElement resolveMaximallySpecificMethodInterface(int depth, String name, MethodDescriptor descriptor) {
        MethodElement candidate = null;
        MethodElement found;
        if (depth > 0) {
            int cnt = getInterfaceCount();
            boolean end = true;
            for (int i = 0; i < cnt; i ++) {
                found = getInterface(i).resolveMaximallySpecificMethodInterface(depth - 1, name, descriptor);
                if (found != null && candidate != null || found == ResolvedTypeDefinitionUtil.NOT_FOUND) {
                    return ResolvedTypeDefinitionUtil.NOT_FOUND;
                }
                if (found != ResolvedTypeDefinitionUtil.END_OF_SEARCH) {
                    end = false;
                }
                candidate = found;
            }
            if (end) {
                return ResolvedTypeDefinitionUtil.END_OF_SEARCH;
            }
            return candidate;
        } else {
            // search *our* interface
            int idx = findMethodIndex(name, descriptor);
            if (idx != -1 && (getMethod(idx).getModifiers() & (ClassFile.ACC_ABSTRACT | ClassFile.ACC_STATIC | ClassFile.ACC_PRIVATE)) == 0) {
                // just one possible candidate at this depth, but it might be overridden so get the virtual handle
                return getMethod(idx);
            } else if (getInterfaceCount() == 0) {
                return ResolvedTypeDefinitionUtil.END_OF_SEARCH;
            } else {
                return null;
            }
        }
    }

    // ==================
    // Constructors
    // ==================

    default int findConstructorIndex(ConstructorDescriptor descriptor) {
        int cnt = getConstructorCount();
        for (int i = 0; i < cnt; i ++) {
            if (getConstructor(i).getDescriptor().equals(descriptor)) {
                return i;
            }
        }
        return -1;
    }

    default ConstructorElement resolveConstructorElement(ConstructorDescriptor descriptor) {
        int idx = findConstructorIndex(descriptor);
        return idx == -1 ? null : getConstructor(idx);
    }
}
