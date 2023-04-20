package org.qbicc.type.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
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

    public static InstanceMethodType resolveInstanceMethodType(DefinedTypeDefinition owner, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature) {
        ClassContext classContext = owner.getContext();
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        // todo: if InlineObject, receiver is just the type...
        ValueType receiverType = owner.load().getObjectType().getReference();
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(owner, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(owner, parameterTypes, paramSignatures, nestedCtxt);
        return classContext.getTypeSystem().getInstanceMethodType(receiverType, resolvedReturnType, resolvedParameterTypes);
    }

    public static StaticMethodType resolveStaticMethodType(DefinedTypeDefinition enclosingType, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature) {
        ClassContext classContext = enclosingType.getContext();
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(enclosingType, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(enclosingType, parameterTypes, paramSignatures, nestedCtxt);
        return classContext.getTypeSystem().getStaticMethodType(resolvedReturnType, resolvedParameterTypes);
    }

    public static FunctionType resolveFunctionType(DefinedTypeDefinition enclosingType, TypeParameterContext tpc, MethodDescriptor descriptor, MethodSignature signature, boolean isVarArg) {
        ClassContext classContext = enclosingType.getContext();
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        if (isVarArg && parameterTypes.size() > 0 && parameterTypes.get(parameterTypes.size() - 1) instanceof ArrayTypeDescriptor) {
            // remove the vararg item
            if (paramSignatures.size() == parameterTypes.size()) {
                paramSignatures = paramSignatures.subList(0, paramSignatures.size() - 1);
            }
            parameterTypes = parameterTypes.subList(0, parameterTypes.size() - 1);
        } else {
            isVarArg = false;
        }
        paramSignatures = computeSignatures(classContext, parameterTypes, paramSignatures);
        TypeParameterContext nestedCtxt = TypeParameterContext.create(tpc, signature);
        ValueType resolvedReturnType = resolveSingleType(enclosingType, returnType, returnTypeSignature, nestedCtxt);
        List<ValueType> resolvedParameterTypes = resolveMultipleTypes(enclosingType, parameterTypes, paramSignatures, nestedCtxt);
        if (isVarArg) {
            final ArrayList<ValueType> list = new ArrayList<>(resolvedParameterTypes);
            list.add(classContext.getTypeSystem().getVariadicType());
            resolvedParameterTypes = list;
        }
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

    public static List<ValueType> resolveMultipleTypes(DefinedTypeDefinition enclosingType, final List<TypeDescriptor> parameterTypes, final List<TypeSignature> paramSignatures, final TypeParameterContext nestedCtxt) {
        int cnt = parameterTypes.size();
        ValueType[] resolvedParamTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            resolvedParamTypes[i] = resolveSingleType(enclosingType, parameterTypes.get(i), paramSignatures.get(i), nestedCtxt);
        }
        return List.of(resolvedParamTypes);
    }

    public static ValueType resolveSingleType(final DefinedTypeDefinition enclosingType, final TypeDescriptor returnType, final TypeSignature returnTypeSignature, final TypeParameterContext nestedCtxt) {
        if (returnType == enclosingType.getDescriptor()) {
            return enclosingType.load().getObjectType().getReference();
        }
        ClassContext classContext = enclosingType.getContext();
        ValueType resolvedReturnType = classContext.resolveTypeFromMethodDescriptor(returnType, nestedCtxt, returnTypeSignature);
        if (resolvedReturnType instanceof ObjectType) {
            resolvedReturnType = ((ObjectType) resolvedReturnType).getReference();
        }
        return resolvedReturnType;
    }
}
