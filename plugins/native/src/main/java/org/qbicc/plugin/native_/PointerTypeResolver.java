package org.qbicc.plugin.native_;

import static org.qbicc.runtime.CNative.*;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.ArrayType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.WordType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.type.TypeAnnotation;
import org.qbicc.type.annotation.type.TypeAnnotationList;
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
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.Variance;

/**
 * This type resolver is responsible for translating pointer types from reference types to actual pointer types.
 */
public class PointerTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public PointerTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
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

    public ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
        boolean restrict = false;
        for (TypeAnnotation typeAnnotation : visibleAnnotations) {
            Annotation annotation = typeAnnotation.getAnnotation();
            if (annotation.getDescriptor().getClassName().equals(Native.ANN_RESTRICT)) {
                restrict = true;
                break;
            }
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
                            pointerType = ctxt.getTypeSystem().getVoidType().getPointer();
                        } else {
                            if (args.size() != 1) {
                                ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                break out;
                            }
                            TypeArgument typeArgument = args.get(0);
                            ValueType pointeeType;
                            visibleAnnotations = visibleAnnotations.onTypeArgument(0);
                            invisibleAnnotations = invisibleAnnotations.onTypeArgument(0);
                            if (typeArgument instanceof AnyTypeArgument) {
                                pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, paramCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                            } else {
                                assert typeArgument instanceof BoundTypeArgument;
                                BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                                if (bound.getVariance() == Variance.INVARIANT) {
                                    // looks right, get the proper pointee type
                                    ReferenceTypeSignature pointeeSig = bound.getBound();
                                    // todo: use context to resolve type variable bounds
                                    TypeDescriptor pointeeDesc = pointeeSig.asDescriptor(classCtxt);
                                    pointeeType = classCtxt.resolveTypeFromDescriptor(pointeeDesc, paramCtxt, pointeeSig, visibleAnnotations, invisibleAnnotations);
                                    if (pointeeType instanceof ReferenceType) {
                                        pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, paramCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                                    }
                                } else {
                                    ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                    pointeeType = classCtxt.resolveTypeFromDescriptor(BaseTypeDescriptor.V, paramCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                                }
                            }
                            pointerType = pointeeType.getPointer();
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        pointerType = ctxt.getTypeSystem().getVoidType().getPointer();
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
            ValueType elemType = classCtxt.resolveTypeFromDescriptor(elemDesc, paramCtxt, elemSig, visibleAnnotations.inArray(), invisibleAnnotations.inArray());
            if (elemType instanceof ArrayType) {
                // it's an array of arrays, make it into a native array instead of a reference array
                return ctxt.getTypeSystem().getArrayType(elemType, detectArraySize(visibleAnnotations));
            } else if (! (elemType instanceof ReferenceType) && elemType instanceof WordType && elemDesc instanceof ClassTypeDescriptor) {
                // this means it's an array of native type (wrapped as ref type) and should be transformed to a "real" array type
                return ctxt.getTypeSystem().getArrayType(elemType, detectArraySize(visibleAnnotations));
            }
            // else fall out
        }
        if (restrict) {
            ctxt.error("Only pointers can have `restrict` qualifier (\"%s\" \"%s\")", descriptor, signature);
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }

    private int detectArraySize(final TypeAnnotationList visibleAnnotations) {
        int arraySize = 0;
        for (TypeAnnotation annotation : visibleAnnotations) {
            ClassTypeDescriptor desc = annotation.getAnnotation().getDescriptor();
            if (desc.getPackageName().equals(Native.NATIVE_PKG) && desc.getClassName().equals(Native.ANN_ARRAY_SIZE)) {
                arraySize = ((IntAnnotationValue) annotation.getAnnotation().getValue("value")).intValue();
            }
        }
        return arraySize;
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return deArray(getDelegate().resolveTypeFromMethodDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations));
    }

    private FunctionType translateFunctionType(FunctionType functionType) {
        ValueType returnType = deArray(functionType.getReturnType());
        int cnt = functionType.getParameterCount();
        ValueType[] paramTypes = new ValueType[cnt];
        for (int i = 0; i < cnt; i ++) {
            paramTypes[i] = deArray(functionType.getParameterType(i));
        }
        return classCtxt.getTypeSystem().getFunctionType(returnType, paramTypes);
    }

    private ValueType deArray(ValueType orig) {
        return orig instanceof ArrayType ? deArray(((ArrayType) orig).getElementType()).getPointer() : orig;
    }
}
