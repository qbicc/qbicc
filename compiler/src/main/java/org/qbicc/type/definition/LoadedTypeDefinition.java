package org.qbicc.type.definition;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.qbicc.type.ClassObjectType;
import org.qbicc.type.InterfaceObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.definition.element.NestedClassElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface LoadedTypeDefinition extends DefinedTypeDefinition {
    default LoadedTypeDefinition load() {
        return this;
    }

    ObjectType getType();

    default ClassObjectType getClassType() {
        return (ClassObjectType) getType();
    }

    default InterfaceObjectType getInterfaceType() {
        return (InterfaceObjectType) getType();
    }

    LoadedTypeDefinition getSuperClass();

    LoadedTypeDefinition getInterface(int index) throws IndexOutOfBoundsException;

    /**
     * Walk the locally declared interfaces
     * 
     * @return an array of LoadedTypeDefinition declared by this class.
     */
    LoadedTypeDefinition[] getInterfaces();

    /**
     * Walk the set of interfaces implemented by this class and its superclasses.
     *
     * Note, interfaces may occur more than once in this walk.
     *
     * @param function the Consumer of the interfaces
     */
    void forEachInterfaceFullImplementedSet(Consumer<LoadedTypeDefinition> function);

    default boolean isSubtypeOf(LoadedTypeDefinition other) {
        return getType().isSubtypeOf(other.getType());
    }

    FieldSet getInstanceFieldSet();

    FieldSet getStaticFieldSet();

    MethodElement[] getInstanceMethods();

    NestedClassElement getEnclosingNestedClass();

    int getEnclosedNestedClassCount();

    NestedClassElement getEnclosedNestedClass(int index) throws IndexOutOfBoundsException;

    FieldElement getField(int index);

    default void eachField(Consumer<FieldElement> consumer) {
        int cnt = getFieldCount();
        for (int i = 0; i < cnt; i ++) {
            consumer.accept(getField(i));
        }
    }

    /**
     * Resolve a field by name and descriptor.
     *
     * @param descriptor the field descriptor (must not be {@code null})
     * @param name the field name (must not be {@code null})
     * @return the field handle, or {@code null} if no matching field is found
     */
    default FieldElement resolveField(TypeDescriptor descriptor, String name) {
        Assert.checkNotNullParam("descriptor", descriptor);
        Assert.checkNotNullParam("name", name);
        // JVMS 5.4.3.2. Field Resolution

        // 1. If C declares a field with the name and descriptor specified by the field reference,
        // field lookup succeeds. The declared field is the result of the field lookup.

        int idx = getFieldIndex(name);
        if (idx >= 0) {
            FieldElement field = getField(idx);
            if (field.getTypeDescriptor().equals(descriptor)) {
                return field;
            } else {
                // no match (wrong type)
                return null;
            }
        }

        // 2. Otherwise, field lookup is applied recursively to the direct superinterfaces
        // of the specified class or interface C.

        int interfaceCount = getInterfaceCount();
        for (int i = 0; i < interfaceCount; i ++) {
            LoadedTypeDefinition each = getInterface(i);
            FieldElement candidate = each.resolveField(descriptor, name);
            if ( candidate != null ) {
                return candidate;
            }
        }

        // 3. Otherwise, if C has a superclass S, field lookup is applied recursively to S.

        LoadedTypeDefinition superType = getSuperClass();
        return superType != null ? superType.resolveField(descriptor, name) : null;
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
            FieldElement field = getField(i);
            if (field.hasNoModifiersOf(ClassFile.I_ACC_NO_RESOLVE) && field.nameEquals(name)) {
                return i;
            }
        }
        return -1;
    }

    default FieldElement findField(String name) {
        int idx = getFieldIndex(name);
        return idx == - 1 ? null : getField(idx);
    }

    void injectField(FieldElement field);

    MethodElement getMethod(int index);

    /**
     * Get the method index of the exactly matching method on this class.  If the method is not directly present on this class,
     * {@code -1} is returned.
     *
     * @param name       the method name (must not be {@code null})
     * @param descriptor the method descriptor (must not be {@code null})
     * @param includePrivate {@code true} to include private methods, or {@code false} to exclude them
     * @return the index of the method, or {@code -1} if it is not present on this class
     */
    default int findMethodIndex(String name, MethodDescriptor descriptor, boolean includePrivate) {
        int cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = getMethod(i);
            if (method.hasAllModifiersOf(ClassFile.I_ACC_NO_RESOLVE) || method.hasAllModifiersOf(ClassFile.ACC_PRIVATE) && ! includePrivate) {
                continue;
            }
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

    /**
     * Get the method index of the exactly matching method on this class.  If the method is not directly present on this class,
     * {@code -1} is returned.
     *
     * @param name       the method name (must not be {@code null})
     * @param descriptor the method descriptor (must not be {@code null})
     * @return the index of the method, or {@code -1} if it is not present on this class
     */
    default int findMethodIndex(String name, MethodDescriptor descriptor) {
        return findMethodIndex(name, descriptor, true);
    }

    default int findMethodIndex(Predicate<MethodElement> predicate) {
        int cnt = getMethodCount();
        for (int i = 0; i < cnt; i ++) {
            MethodElement method = getMethod(i);
            if (predicate.test(method)) {
                return i;
            }
        }
        return -1;
    }

    default MethodElement resolveMethodElementExact(String name, MethodDescriptor descriptor) {
        int idx = findMethodIndex(name, descriptor);
        return idx == -1 ? null : getMethod(idx);
    }

    default MethodElement resolveMethodElementVirtual(String name, MethodDescriptor descriptor) {
        return resolveMethodElementVirtual(name, descriptor, true);
    }

    default MethodElement resolveMethodElementVirtual(String name, MethodDescriptor descriptor, boolean includePrivate) {
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

        int result = findMethodIndex(name, descriptor, includePrivate);
        if (result != -1) {
            return getMethod(result);
        }

        // 2.c Otherwise, if C has a superclass, step 2 of method resolution is recursively
        // invoked on the direct superclass of C.
        //
        // We exclude private methods from this search because they cannot be inherited.

        LoadedTypeDefinition superClass = getSuperClass();
        if ( superClass != null ) {
            MethodElement superCandidate = superClass.resolveMethodElementVirtual(name, descriptor, false);
            if ( superCandidate != null ) {
                return superCandidate;
            }
        }

        // We do not need to search interfaces because we've already registered interface methods onto the class.

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

        int result = findMethodIndex(name, descriptor, ! virtualOnly);
        if (result != -1) {
            return getMethod(result);
        }

        // 3. Otherwise, if the class Object declares a method with the name and descriptor
        // specified by the interface method reference, which has its ACC_PUBLIC flag set
        // and does not have its ACC_STATIC flag set, method lookup succeeds.
        if (! virtualOnly) {
            LoadedTypeDefinition object = getContext().findDefinedType("java/lang/Object").load();
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
            if (found == MethodElement.NOT_FOUND || found == MethodElement.END_OF_SEARCH) {
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
                if (found != null && candidate != null || found == MethodElement.NOT_FOUND) {
                    return MethodElement.NOT_FOUND;
                }
                if (found != MethodElement.END_OF_SEARCH) {
                    end = false;
                }
                candidate = found;
            }
            if (end) {
                return MethodElement.END_OF_SEARCH;
            }
            return candidate;
        } else {
            // search *our* interface
            int idx = findMethodIndex(name, descriptor);
            if (idx != -1 && (getMethod(idx).getModifiers() & (ClassFile.ACC_ABSTRACT | ClassFile.ACC_STATIC | ClassFile.ACC_PRIVATE)) == 0) {
                // just one possible candidate at this depth, but it might be overridden so get the virtual handle
                return getMethod(idx);
            } else if (getInterfaceCount() == 0) {
                return MethodElement.END_OF_SEARCH;
            } else {
                return null;
            }
        }
    }




    ConstructorElement getConstructor(int index);

    default int findConstructorIndex(MethodDescriptor descriptor) {
        int cnt = getConstructorCount();
        for (int i = 0; i < cnt; i ++) {
            if (getConstructor(i).getDescriptor().equals(descriptor)) {
                return i;
            }
        }
        return -1;
    }

    default ConstructorElement resolveConstructorElement(MethodDescriptor descriptor) {
        int idx = findConstructorIndex(descriptor);
        return idx == -1 ? null : getConstructor(idx);
    }

    InitializerElement getInitializer();


    /**
     * Get this ValidatedTypeDefinition's typeId.
     * 
     * Prior to TypeIds being assigned in the 
     * Post ANALAZE phase, this method will return an
     * invalid typeId, likely -1.
     */
    int getTypeId();

    /**
     * Get the highest numeric valued typeId that represents
     * a valid subclass of this ValidatedTypeDefinition.
     * 
     * For a leaf class, this will be equal to #getTypeId().
     * For a class with subclasses, this will be equal to the
     * highest valued typeid of a subclass.
     * 
     * Ex:
     * ```
     * class I {}
     * class J extends I {}
     * class K extends J {}
     * class L extends I {}
     * ```
     * We will visit each subclass and assign them typeIds. One
     * such assignment is:
     * ```
     * I : 1
     * J : 2
     * K : 3
     * L : 4
     * ```
     * where I will have typeId 1 and maximumSubtypeId 4 allowing
     * subtype checks by validating if their typeId, x, satisifies
     * the relationship 1 <= x <= 4
     * 
     * If it is not yet assigned, it will return `-1`.
     */
    int getMaximumSubtypeId();

    /**
     * TypeIds are assigned late in the process and may
     * not be valid yet.  This method allows checking if
     * the typeId has been assigned before attempting to
     * use it.
     * 
     * By default, "-1" is used as an invalid typeId.
     */
    boolean isTypeIdValid();

    /**
     * Assign the typeId to this Class or Interface.
     * This can only be done once.
     */
    void assignTypeId(int myTypeId);

    /**
     * Assign the maximumSubtypeId to this Class or Interface.
     * This can only be done once.
     */
    void assignMaximumSubtypeId(int subTypeId);

    /**
     * Whether this class declares default (non-abstract, non-static) methods.
     * This is always false for a Class.  For an interface, this is only true
     * if it declares such a method itself.
     * 
     * @return true if a default method is declared on this interface.  False otherwise
     */
    boolean declaresDefaultMethods();

    /**
     * Whether this class declares or inherits default (non-abstract, non-static) methods.
     * For a class, this is true if any of the interfaces it implements, including their supers,
     * declares a default method.
     * For an interface, this is true if #declaresDefaultMethods() is true for itself or its supers.
     * 
     * @return whether it has default methods.
     */
    boolean hasDefaultMethods();

}
