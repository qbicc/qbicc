package org.qbicc.type.util;

import java.util.Arrays;
import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 * Utilities to help with resolving descriptors.
 */
public final class ResolutionUtil {
    private ResolutionUtil() {}

    public static InstanceMethodType resolveInstanceMethodType(final ClassContext classContext, DefinedTypeDefinition owner, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature) {
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        // todo: if InlineObject, receiver is just the type...
        ValueType receiverType = owner.load().getType().getReference();
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(classContext, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(classContext, parameterTypes, paramSignatures, nestedCtxt);
        return classContext.getTypeSystem().getInstanceMethodType(receiverType, resolvedReturnType, resolvedParameterTypes);
    }

    public static StaticMethodType resolveStaticMethodType(final ClassContext classContext, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature) {
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(classContext, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(classContext, parameterTypes, paramSignatures, nestedCtxt);
        return classContext.getTypeSystem().getStaticMethodType(resolvedReturnType, resolvedParameterTypes);
    }

    public static FunctionType resolveFunctionType(final ClassContext classContext, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature) {
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(classContext, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(classContext, parameterTypes, paramSignatures, nestedCtxt);
        return classContext.getTypeSystem().getFunctionType(resolvedReturnType, resolvedParameterTypes);
    }

    public static List<TypeSignature> computeSignatures(final ClassContext classContext, final List<TypeDescriptor> parameterTypes, List<TypeSignature> paramSignatures) {
        int cnt = parameterTypes.size();
        if (paramSignatures.size() != cnt) {
            // sig-poly or bad generic data
            TypeSignature[] array = new TypeSignature[cnt];
            for (int i = 0; i < cnt; i ++) {
                array[i] = TypeSignature.synthesize(classContext, parameterTypes.get(i));
            }
            paramSignatures = Arrays.asList(array);
        }
        return paramSignatures;
    }

    public static List<ValueType> resolveMultipleTypes(final ClassContext classContext, final List<TypeDescriptor> parameterTypes, final List<TypeSignature> paramSignatures, final TypeParameterContext nestedCtxt) {
        int cnt = parameterTypes.size();
        ValueType[] resolvedParamTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            resolvedParamTypes[i] = classContext.resolveTypeFromMethodDescriptor(parameterTypes.get(i), nestedCtxt, paramSignatures.get(i), TypeAnnotationList.empty(), TypeAnnotationList.empty());
            if (resolvedParamTypes[i] instanceof ObjectType) {
                resolvedParamTypes[i] = ((ObjectType) resolvedParamTypes[i]).getReference();
            }
        }
        return List.of(resolvedParamTypes);
    }

    public static ValueType resolveSingleType(final ClassContext classContext, final TypeDescriptor returnType, final TypeSignature returnTypeSignature, final TypeParameterContext nestedCtxt) {
        ValueType resolvedReturnType = classContext.resolveTypeFromMethodDescriptor(returnType, nestedCtxt, returnTypeSignature, TypeAnnotationList.empty(), TypeAnnotationList.empty());
        if (resolvedReturnType instanceof ObjectType) {
            resolvedReturnType = ((ObjectType) resolvedReturnType).getReference();
        }
        return resolvedReturnType;
    }
}
