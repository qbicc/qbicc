package org.qbicc.plugin.native_;

import static org.qbicc.runtime.CNative.*;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.runtime.CNative;
import org.qbicc.type.ArrayType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.AnnotationValue;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.descriptor.ArrayTypeDescriptor;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.AnyTypeArgument;
import org.qbicc.type.generic.ArrayTypeSignature;
import org.qbicc.type.generic.BaseTypeSignature;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.TopLevelClassTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameter;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.TypeVariableSignature;
import org.qbicc.type.generic.Variance;

/**
 * This type resolver is responsible for translating pointer types from reference types to actual pointer types.
 */
public class PointerTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassTypeDescriptor restrictAnnotation;
    private final ClassTypeDescriptor arraySizeAnnotation;
    private final ClassTypeDescriptor addrSpaceAnnotation;

    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public PointerTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
        restrictAnnotation = ClassTypeDescriptor.synthesize(classCtxt, Native.RESTRICT_INT_NAME);
        arraySizeAnnotation = ClassTypeDescriptor.synthesize(classCtxt, Native.ARRAY_SIZE_INT_NAME);
        addrSpaceAnnotation = ClassTypeDescriptor.synthesize(classCtxt, Native.ADDR_SPACE_INT_NAME);
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        if (packageName.equals(Native.NATIVE_PKG)) {
            if (internalName.equals(Native.PTR)) {
                return ctxt.getTypeSystem().getVoidType().getPointer();
            }
        }
        return getDelegate().resolveTypeFromClassName(packageName, internalName);
    }

    public ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature) {
        boolean restrict = signature.hasAnnotation(restrictAnnotation);
        int addrSpace = 0;
        if (signature.hasAnnotation(addrSpaceAnnotation)) {
            addrSpace = ((IntAnnotationValue)signature.getAnnotation(addrSpaceAnnotation).getValue("value")).intValue();
        }
        // special handling for pointers and functions
        out: if (descriptor instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor ctd = (ClassTypeDescriptor) descriptor;
            if (ctd.getPackageName().equals(Native.NATIVE_PKG)) {
                if (ctd.getClassName().equals(Native.PTR)) {
                    PointerType pointerType;
                    // check the signature
                    if (signature instanceof ClassTypeSignature) {
                        ClassTypeSignature sig = (ClassTypeSignature) signature;
                        if (! sig.getIdentifier().equals(Native.PTR)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", ptr.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() == 0) {
                            pointerType = ctxt.getTypeSystem().getVoidType().pointer(addrSpace);
                        } else {
                            if (args.size() != 1) {
                                ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                break out;
                            }
                            TypeArgument typeArgument = args.get(0);
                            ValueType pointeeType;
                            if (typeArgument instanceof AnyTypeArgument) {
                                pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, paramCtxt, BaseTypeSignature.V);
                            } else {
                                assert typeArgument instanceof BoundTypeArgument;
                                BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                                if (bound.getVariance() == Variance.INVARIANT) {
                                    // looks right, get the proper pointee type
                                    ReferenceTypeSignature pointeeSig = bound.getBound();
                                    // todo: use context to resolve type variable bounds
                                    TypeDescriptor pointeeDesc = pointeeSig.asDescriptor(classCtxt);
                                    pointeeType = classCtxt.resolveTypeFromDescriptor(pointeeDesc, paramCtxt, pointeeSig);
                                } else {
                                    ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                    pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, paramCtxt, BaseTypeSignature.V);
                                }
                            }
                            pointerType = pointeeType.pointer(addrSpace);
                        }
                    } else if (signature instanceof TypeVariableSignature tvs) {
                        TypeParameter resolved = paramCtxt.resolveTypeParameter(tvs.getIdentifier());
                        ReferenceTypeSignature classBound = resolved.getClassBound();
                        if (classBound instanceof TopLevelClassTypeSignature sig && sig.getIdentifier().equals(Native.PTR)) {
                            return resolveTypeFromMethodDescriptor(descriptor, paramCtxt, sig);
                        }
                        // the pointer type is just unknown
                        pointerType = ctxt.getTypeSystem().getVoidType().pointer(addrSpace);
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        pointerType = ctxt.getTypeSystem().getVoidType().pointer(addrSpace);
                    }
                    return restrict ? pointerType.asRestrict() : pointerType;
                } else {
                    break out;
                }
            } else {
                break out;
            }
        } else if (descriptor instanceof ArrayTypeDescriptor) {
            TypeDescriptor elemDesc = ((ArrayTypeDescriptor) descriptor).getElementTypeDescriptor();
            TypeSignature elemSig = signature instanceof ArrayTypeSignature ? ((ArrayTypeSignature) signature).getElementTypeSignature() : TypeSignature.synthesize(classCtxt, elemDesc);
            // resolve element type
            ValueType elemType = classCtxt.resolveTypeFromDescriptor(elemDesc, paramCtxt, elemSig);
            if (elemType instanceof ArrayType) {
                // it's an array of arrays, make it into a native array instead of a reference array
                return ctxt.getTypeSystem().getArrayType(elemType, detectArraySize(signature));
            } else if (! (elemType instanceof ObjectType) && elemDesc instanceof ClassTypeDescriptor) {
                // this means it's an array of native type (wrapped as ref type) and should be transformed to a "real" array type
                return ctxt.getTypeSystem().getArrayType(elemType, detectArraySize(signature));
            } else if (elemDesc instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals(Native.NATIVE_PKG, Native.REFERENCE)) {
                // it's a native array of Java references
                return ctxt.getTypeSystem().getArrayType(((ObjectType) elemType).getReference(), detectArraySize(signature));
            }
            // else fall out
        }
        if (restrict) {
            ctxt.error("Only pointers can have `restrict` qualifier (\"%s\" \"%s\")", descriptor, signature);
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }

    private int detectArraySize(final TypeSignature visibleAnnotations) {
        Annotation annotation = visibleAnnotations.getAnnotation(arraySizeAnnotation);
        if (annotation == null) {
            return 0;
        }
        AnnotationValue value = annotation.getValue("value");
        if (value instanceof IntAnnotationValue iav) {
            return iav.intValue();
        } else {
            return 0;
        }
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        return deArray(getDelegate().resolveTypeFromMethodDescriptor(descriptor, paramCtxt, signature));
    }

    private ValueType deArray(ValueType orig) {
        return orig instanceof ArrayType ? deArray(((ArrayType) orig).getElementType()).getPointer() : orig;
    }
}
