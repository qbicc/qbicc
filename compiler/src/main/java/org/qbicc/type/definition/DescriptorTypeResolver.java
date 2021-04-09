package org.qbicc.type.definition;

import java.util.List;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.MethodSignature;
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
     * @param visibleAnnotations the visible type annotations list in this use site (must not be {@code null})
     * @param invisibleAnnotations the invisible type annotations list in this use site (must not be {@code null})
     * @return the resolved type
     */
    ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations);

    /**
     * Resolve a method descriptor to a function type.  The method descriptor is generated from the class file.
     * The type parameter context can be used to evaluate the bounds of type variable signature elements.
     * The signature is either generated from the class file or synthesized.
     * The annotation lists contain the type annotations present on the use site of the type (one list entry per method parameter).
     *
     * @param descriptor the method descriptor (must not be {@code null})
     * @param paramCtxt the type parameter context (must not be {@code null})
     * @param signature the method signature (must not be {@code null})
     * @param returnTypeVisible the visible type annotations list on the return type of this use site (must not be {@code null})
     * @param visibleAnnotations the invisible type annotations list on the return type of this use site (must not be {@code null})
     * @param returnTypeInvisible the list of visible type annotations list on the parameters of this use site (must not be {@code null})
     * @param invisibleAnnotations the list of visible type annotations list on the parameters of this use site (must not be {@code null})
     * @return the resolved function type
     */
    FunctionType resolveMethodFunctionType(MethodDescriptor descriptor, TypeParameterContext paramCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, List<TypeAnnotationList> invisibleAnnotations);

    ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations);

    ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible);
        
    interface Delegating extends DescriptorTypeResolver {
        DescriptorTypeResolver getDelegate();

        default ValueType resolveTypeFromClassName(String packageName, String internalName) {
            return getDelegate().resolveTypeFromClassName(packageName, internalName);
        }

        default ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
            return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations);
        }

        default public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
            return getDelegate().resolveArrayObjectTypeFromDescriptor(descriptor, paramCtxt, signature, visible, invisible);
        }

        default FunctionType resolveMethodFunctionType(MethodDescriptor descriptor, TypeParameterContext paramCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, List<TypeAnnotationList> invisibleAnnotations) {
            return getDelegate().resolveMethodFunctionType(descriptor, paramCtxt, signature, returnTypeVisible, visibleAnnotations, returnTypeInvisible, invisibleAnnotations);
        }

        default ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
            return getDelegate().resolveTypeFromMethodDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations);
        }
    }
}
