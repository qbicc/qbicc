package org.qbicc.type.definition;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ValueType;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 * A resolver which converts descriptors into usable types.
 */
public interface DescriptorTypeResolver {
    /**
     * Resolve a type from a package and class name.
     *
     * @param packageName the package name (must not be {@code null})
     * @param internalName the class internal name (must not be {@code null})
     * @return the resolved type
     */
    ValueType resolveTypeFromClassName(String packageName, String internalName);

    /**
     * Resolve a type.  The type descriptor is generated from the class file.  The type parameter context can be used
     * to evaluate the bounds of type variable signature elements.  The signature is either generated from the class
     * file or synthesized.  The annotation lists contain the type annotations present on the use site of the type.
     *
     * @param descriptor the type descriptor (must not be {@code null})
     * @param paramCtxt the type parameter context (must not be {@code null})
     * @param signature the type signature (must not be {@code null})
     * @return the resolved type
     */
    ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature);

    ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature);

    ArrayObjectType resolveArrayObjectTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature);

    interface Delegating extends DescriptorTypeResolver {
        DescriptorTypeResolver getDelegate();

        default ValueType resolveTypeFromClassName(String packageName, String internalName) {
            return getDelegate().resolveTypeFromClassName(packageName, internalName);
        }

        default ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature) {
            return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
        }

        default ArrayObjectType resolveArrayObjectTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature) {
            return getDelegate().resolveArrayObjectTypeFromDescriptor(descriptor, paramCtxt, signature);
        }

        default ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature) {
            return getDelegate().resolveTypeFromMethodDescriptor(descriptor, paramCtxt, signature);
        }
    }
}
