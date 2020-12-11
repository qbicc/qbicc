package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.runtime.CNative.*;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ArrayType;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.PointerType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.descriptor.BaseTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.AnyTypeArgument;
import cc.quarkus.qcc.type.generic.BaseTypeSignature;
import cc.quarkus.qcc.type.generic.BoundTypeArgument;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.NestedClassTypeSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.ReferenceTypeSignature;
import cc.quarkus.qcc.type.generic.TypeArgument;
import cc.quarkus.qcc.type.generic.TypeSignature;
import cc.quarkus.qcc.type.generic.Variance;

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

    public ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, List<ParameterizedSignature> typeParamCtxt, TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
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
                    if (signature instanceof NestedClassTypeSignature) {
                        NestedClassTypeSignature sig = (NestedClassTypeSignature) signature;
                        if (! sig.getIdentifier().equals("ptr")) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", ptr.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 1) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        ValueType pointeeType;
                        visibleAnnotations = visibleAnnotations.onTypeArgument(0);
                        invisibleAnnotations = invisibleAnnotations.onTypeArgument(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            pointeeType = resolveTypeFromDescriptor(BaseTypeDescriptor.V, typeParamCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() == Variance.INVARIANT) {
                                // looks right, get the proper pointee type
                                ReferenceTypeSignature pointeeSig = bound.getBound();
                                // todo: use context to resolve type variable bounds
                                TypeDescriptor pointeeDesc = pointeeSig.asDescriptor(classCtxt);
                                pointeeType = resolveTypeFromDescriptor(pointeeDesc, typeParamCtxt, pointeeSig, visibleAnnotations, invisibleAnnotations);
                            } else {
                                ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", ptr.class, sig);
                                pointeeType = resolveTypeFromDescriptor(BaseTypeDescriptor.V, typeParamCtxt, BaseTypeSignature.V, visibleAnnotations, invisibleAnnotations);
                            }
                        }
                        pointerType = pointeeType.getPointer();
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", NestedClassTypeSignature.class, signature.getClass());
                        pointerType = ctxt.getTypeSystem().getVoidType().getPointer();
                    }
                    return restrict ? pointerType.asRestrict() : pointerType;
                } else {
                    break out;
                }
            } else {
                break out;
            }
        }
        if (restrict) {
            ctxt.error("Only pointers can have `restrict` qualifier (\"%s\" \"%s\")", descriptor, signature);
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }

    public FunctionType resolveTypeFromMethodDescriptor(final MethodDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations) {
        // special handling for native arrays which are always pointers in function types
        FunctionType functionType = getDelegate().resolveTypeFromMethodDescriptor(descriptor, typeParamCtxt, signature, returnTypeVisible, visibleAnnotations, returnTypeInvisible, invisibleAnnotations);
        if (functionType.getReturnType() instanceof ArrayType) {
            // this wouldn't actually be possible in C, but let's make it into a pointer anyway
            return translateFunctionType(functionType);
        }
        int cnt = functionType.getParameterCount();
        for (int i = 0; i < cnt; i ++) {
            ValueType parameterType = functionType.getParameterType(i);
            if (parameterType instanceof ArrayType) {
                return translateFunctionType(functionType);
            }
        }
        return functionType;
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
        return orig instanceof ArrayType ? ((ArrayType) orig).getElementType().getPointer() : orig;
    }
}
