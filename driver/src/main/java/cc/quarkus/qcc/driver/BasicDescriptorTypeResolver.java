package cc.quarkus.qcc.driver;

import java.util.List;

import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.InterfaceObjectType;
import cc.quarkus.qcc.type.PhysicalObjectType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
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
import cc.quarkus.qcc.type.generic.TypeParameterContext;
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

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
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
            ArrayObjectType arrayObjectType = resolveArrayObjectTypeFromDescriptor(descriptor, paramCtxt, signature, visible, invisible);
            return arrayObjectType.getReference();
        }
     }

    public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
        if (descriptor instanceof BaseTypeDescriptor) {
            throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
        } else if (descriptor instanceof ClassTypeDescriptor) {
            throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
        } else {
            assert descriptor instanceof ArrayTypeDescriptor;
            TypeDescriptor elemDescriptor = ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor();
            TypeSystem ts = classContext.getTypeSystem();
            if (elemDescriptor instanceof BaseTypeDescriptor) {
                switch (((BaseTypeDescriptor) elemDescriptor).getShortName()) {
                    case 'B': return ts.getSignedInteger8Type().getPrimitiveArrayObjectType();
                    case 'C': return ts.getUnsignedInteger16Type().getPrimitiveArrayObjectType();
                    case 'D': return ts.getFloat64Type().getPrimitiveArrayObjectType();
                    case 'F': return ts.getFloat32Type().getPrimitiveArrayObjectType();
                    case 'I': return ts.getSignedInteger32Type().getPrimitiveArrayObjectType();
                    case 'J': return ts.getSignedInteger64Type().getPrimitiveArrayObjectType();
                    case 'S': return ts.getSignedInteger16Type().getPrimitiveArrayObjectType();
                    case 'Z': return ts.getBooleanType().getPrimitiveArrayObjectType();
                    default: throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
                }
            } else if (elemDescriptor instanceof ClassTypeDescriptor) {
                ValueType elemType = classContext.resolveTypeFromClassName(((ClassTypeDescriptor)elemDescriptor).getPackageName(), ((ClassTypeDescriptor)elemDescriptor).getClassName());
                if (elemType instanceof ReferenceType) {
                    ReferenceType refElemType = (ReferenceType) elemType;
                    if (refElemType.getInterfaceBounds().size() > 0) {
                        assert refElemType.getInterfaceBounds().size() == 1;
                        InterfaceObjectType typeDef = refElemType.getInterfaceBounds().iterator().next();
                        return typeDef.getReferenceArrayObject();
                    } else {
                        PhysicalObjectType typeDef = refElemType.getUpperBound();
                        return typeDef.getReferenceArrayObject();
                    }
                } else {
                    throw new ResolutionFailedException("Cannot resolve type as array " + descriptor);
                }
            } else {
                TypeSignature elemSig;
                if (signature instanceof ArrayTypeSignature) {
                    elemSig = ((ArrayTypeSignature) signature).getElementTypeSignature();
                } else {
                    elemSig = TypeSignature.synthesize(classContext, elemDescriptor);
                }
                ArrayObjectType elementArrayObj = resolveArrayObjectTypeFromDescriptor(elemDescriptor, paramCtxt, elemSig, visible, invisible);
                return elementArrayObj.getReferenceArrayObject();
            }
        }
    }

    public FunctionType resolveMethodFunctionType(final MethodDescriptor descriptor, TypeParameterContext paramCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visible, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisible) {
        TypeDescriptor returnType = descriptor.getReturnType();
        List<TypeDescriptor> parameterTypes = descriptor.getParameterTypes();
        TypeSignature returnTypeSignature = signature.getReturnTypeSignature();
        List<TypeSignature> paramSignatures = signature.getParameterTypes();
        TypeParameterContext nestedCtxt = TypeParameterContext.create(paramCtxt, signature);
        ValueType resolvedReturnType = classContext.resolveTypeFromMethodDescriptor(returnType, nestedCtxt, returnTypeSignature, returnTypeVisible, returnTypeInvisible);
        int cnt = parameterTypes.size();
        ValueType[] resolvedParamTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            resolvedParamTypes[i] = classContext.resolveTypeFromMethodDescriptor(parameterTypes.get(i), nestedCtxt, paramSignatures.get(i), visible.get(i), invisible.get(i));
        }
        return classContext.getTypeSystem().getFunctionType(resolvedReturnType, resolvedParamTypes);
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return classContext.resolveTypeFromDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }
}
