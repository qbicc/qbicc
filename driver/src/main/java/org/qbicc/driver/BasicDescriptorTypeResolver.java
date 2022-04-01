package org.qbicc.driver;

import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.ResolutionFailedException;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.ArrayTypeSignature;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

final class BasicDescriptorTypeResolver implements DescriptorTypeResolver {
    private final ClassContext classContext;

    BasicDescriptorTypeResolver(final ClassContext classContext) {
        this.classContext = classContext;
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        DefinedTypeDefinition definedType = classContext.findDefinedType(packageName.isEmpty() ? internalName : packageName + '/' + internalName);
        if (definedType == null) {
            return null;
        } else {
            return definedType.load().getObjectType();
        }
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
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
            ArrayObjectType arrayObjectType = resolveArrayObjectTypeFromDescriptor(descriptor, paramCtxt, signature);
            return arrayObjectType;
        }
     }

    public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
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
                if (elemType instanceof ObjectType) {
                    return ((ObjectType) elemType).getReferenceArrayObject();
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
                ArrayObjectType elementArrayObj = resolveArrayObjectTypeFromDescriptor(elemDescriptor, paramCtxt, elemSig);
                return elementArrayObj.getReferenceArrayObject();
            }
        }
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        return classContext.resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }
}
