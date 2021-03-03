package cc.quarkus.qcc.type.definition;

import java.util.List;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

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
     * @param typeParamCtxt the enclosing context of parameterized types (must not be {@code null})
     * @param signature the type signature (must not be {@code null})
     * @param visibleAnnotations the visible type annotations list in this use site (must not be {@code null})
     * @param invisibleAnnotations the invisible type annotations list in this use site (must not be {@code null})
     * @return the resolved type
     */
    ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations);

    /**
     * Resolve a method descriptor to a function type.  The method descriptor is generated from the class file.
     * The type parameter context can be used to evaluate the bounds of type variable signature elements.
     * The signature is either generated from the class file or synthesized.
     * The annotation lists contain the type annotations present on the use site of the type (one list entry per method parameter).
     *
     * @param descriptor the method descriptor (must not be {@code null})
     * @param typeParamCtxt the enclosing context of parameterized types (must not be {@code null})
     * @param signature the method signature (must not be {@code null})
     * @param returnTypeVisible the visible type annotations list on the return type of this use site (must not be {@code null})
     * @param visibleAnnotations the invisible type annotations list on the return type of this use site (must not be {@code null})
     * @param returnTypeInvisible the list of visible type annotations list on the parameters of this use site (must not be {@code null})
     * @param invisibleAnnotations the list of visible type annotations list on the parameters of this use site (must not be {@code null})
     * @return the resolved function type
     */
    FunctionType resolveMethodFunctionType(MethodDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, List<TypeAnnotationList> invisibleAnnotations);

    ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations);

    ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible);
        
    interface Delegating extends DescriptorTypeResolver {
        DescriptorTypeResolver getDelegate();

        default ValueType resolveTypeFromClassName(String packageName, String internalName) {
            return getDelegate().resolveTypeFromClassName(packageName, internalName);
        }

        default ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
            return getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
        }

        default public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
            return getDelegate().resolveArrayObjectTypeFromDescriptor(descriptor, typeParamCtxt, signature, visible, invisible);
        }

        default FunctionType resolveMethodFunctionType(MethodDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, List<TypeAnnotationList> invisibleAnnotations) {
            return getDelegate().resolveMethodFunctionType(descriptor, typeParamCtxt, signature, returnTypeVisible, visibleAnnotations, returnTypeInvisible, invisibleAnnotations);
        }

        default ValueType resolveTypeFromMethodDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
            return getDelegate().resolveTypeFromMethodDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
        }
    }
}
