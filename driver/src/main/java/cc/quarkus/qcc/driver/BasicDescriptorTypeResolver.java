package cc.quarkus.qcc.driver;

import java.util.Arrays;
import java.util.List;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.ReferenceArrayObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.WordType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.definition.ResolutionFailedException;
import cc.quarkus.qcc.type.descriptor.ArrayTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.ArrayTypeSignature;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

final class BasicDescriptorTypeResolver implements DescriptorTypeResolver {
    private final ClassContext classContext;

    BasicDescriptorTypeResolver(final ClassContext classContext) {
        this.classContext = classContext;
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        DefinedTypeDefinition definedType = classContext.findDefinedType(packageName == "" ? internalName : packageName + '/' + internalName);
        if (definedType == null) {
            return null;
        } else {
            return definedType.validate().getType().getReference();
        }
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
        TypeSystem ts = classContext.getCompilationContext().getTypeSystem();
        if (descriptor instanceof BaseTypeDescriptor) {
            switch (((BaseTypeDescriptor) descriptor).getShortName()) {
                case 'B': return ts.getSignedInteger8Type();
                case 'C': return ts.getUnsignedInteger16Type();
                case 'D': return ts.getFloat64Type();
                case 'F': return ts.getFloat32Type();
                case 'I': return ts.getSignedInteger32Type();
                case 'J': return ts.getSignedInteger64Type();
                case 'S': return ts.getSignedInteger16Type();
                case 'V': return ts.getVoidType();
                case 'Z': return ts.getBooleanType();
            }
            throw new ResolutionFailedException("Cannot resolve type " + descriptor);
        } else if (descriptor instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor classTypeDescriptor = (ClassTypeDescriptor) descriptor;
            return classContext.resolveTypeFromClassName(classTypeDescriptor.getPackageName(), classTypeDescriptor.getClassName());
        } else {
            assert descriptor instanceof ArrayTypeDescriptor;
            TypeDescriptor elemType = ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor();
            TypeSignature elemSig;
            if (signature instanceof ArrayTypeSignature) {
                elemSig = ((ArrayTypeSignature) signature).getElementTypeSignature();
            } else {
                elemSig = TypeSignature.synthesize(classContext, elemType);
            }
            ValueType elementType = classContext.resolveTypeFromDescriptor(elemType, typeParamCtxt, elemSig, visible.inArray(), invisible.inArray());
            return elementType instanceof ReferenceType ? ((ReferenceType) elementType).getReferenceArrayObject().getReference() : ((WordType) elementType).getPrimitiveArrayObjectType().getReference();
        }
    }

    public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
        if (descriptor instanceof BaseTypeDescriptor) {
            throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
        } else if (descriptor instanceof ClassTypeDescriptor) {
            throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
        } else {
            assert descriptor instanceof ArrayTypeDescriptor;
            TypeDescriptor elemType = ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor();
            TypeSignature elemSig;
            if (signature instanceof ArrayTypeSignature) {
                elemSig = ((ArrayTypeSignature) signature).getElementTypeSignature();
            } else {
                elemSig = TypeSignature.synthesize(classContext, elemType);
            }
            ValueType elementType = classContext.resolveTypeFromDescriptor(elemType, typeParamCtxt, elemSig, visible.inArray(), invisible.inArray());
            return elementType instanceof ReferenceType ? ((ReferenceType) elementType).getReferenceArrayObject() : ((WordType) elementType).getPrimitiveArrayObjectType();
        }
    }

    public FunctionType resolveMethodFunctionType(final MethodDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visible, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisible) {
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        List<ParameterizedSignature> nestedCtxt;
        if (signature.getTypeParameters().isEmpty()) {
            // no nested context needed
            nestedCtxt = typeParamCtxt;
        } else {
            int ctxtSize = typeParamCtxt.size();
            if (ctxtSize == 0) {
                nestedCtxt = List.of(signature);
            } else {
                ParameterizedSignature[] array = new ParameterizedSignature[ctxtSize + 1];
                typeParamCtxt.toArray(array);
                array[ctxtSize] = signature;
                nestedCtxt = Arrays.asList(array);
            }
        }
        ValueType resolvedReturnType = classContext.resolveTypeFromMethodDescriptor(returnType, nestedCtxt, returnTypeSignature, returnTypeVisible, returnTypeInvisible);
        int cnt = parameterTypes.size();
        ValueType[] resolvedParamTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            resolvedParamTypes[i] = classContext.resolveTypeFromMethodDescriptor(parameterTypes.get(i), nestedCtxt, paramSignatures.get(i), visible.get(i), invisible.get(i));
        }
        return classContext.getTypeSystem().getFunctionType(resolvedReturnType, resolvedParamTypes);
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return classContext.resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }
}
