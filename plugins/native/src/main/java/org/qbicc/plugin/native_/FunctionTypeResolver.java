package org.qbicc.plugin.native_;

import static org.qbicc.runtime.CNative.*;

import java.util.List;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.StaticMethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.AnyTypeArgument;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.Variance;

/**
 * A type resolver which resolves function types.
 */
public class FunctionTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public FunctionTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        out: if (descriptor instanceof ClassTypeDescriptor ctd) {
            if (ctd.getPackageName().equals(Native.NATIVE_PKG)) {
                if (ctd.getClassName().equals(Native.FUNCTION)) {
                    FunctionType functionType;
                    // check the signature
                    TypeSystem ts = ctxt.getTypeSystem();
                    if (signature instanceof ClassTypeSignature sig) {
                        if (! sig.getIdentifier().equals(Native.FUNCTION)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", function.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 1) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", function.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            ctxt.error("Function types cannot be open-ended");
                            return ts.getFunctionType(ts.getVoidType(), List.of());
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() != Variance.INVARIANT) {
                                ctxt.error("Function types cannot be open-ended");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            // looks right, get the proper pointee type
                            ReferenceTypeSignature pointeeSig = bound.getBound();
                            if (! (pointeeSig instanceof ClassTypeSignature ctsBound)) {
                                ctxt.error("Function pointee must be a functional interface");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            // todo: use context to resolve type variable bounds
                            ClassTypeDescriptor classDesc = ctsBound.asDescriptor(classCtxt);
                            final String name;
                            if (classDesc.getPackageName().isEmpty()) {
                                name = classDesc.getClassName();
                            } else {
                                name = classDesc.getPackageName() + '/' + classDesc.getClassName();
                            }
                            if (name.equals("java/lang/Object")) {
                                // special case: it's really an "any" pointer with extra front-end guards on it
                                ctxt.error("Function types cannot be open-ended");
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            }
                            DefinedTypeDefinition definedType = classCtxt.findDefinedType(name);
                            if (definedType == null) {
                                ctxt.error("Function interface type \"%s\" is missing", name);
                                return ts.getFunctionType(ts.getVoidType(), List.of());
                            } else {
                                if (definedType.isInterface()) {
                                    return NativeInfo.get(ctxt).getInterfaceAsFunctionType(definedType);
                                } else {
                                    ctxt.error("Function interface type \"%s\" is not an interface", name);
                                    return ts.getFunctionType(ts.getVoidType(), List.of());
                                }
                            }
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        functionType = ts.getFunctionType(ts.getVoidType(), List.of());
                    }
                    return functionType;
                } else if (ctd.getClassName().equals(Native.STATIC_METHOD)) {
                    StaticMethodType staticMethodType;
                    // check the signature
                    TypeSystem ts = ctxt.getTypeSystem();
                    if (signature instanceof ClassTypeSignature sig) {
                        if (! sig.getIdentifier().equals(Native.STATIC_METHOD)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", static_method.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 1) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", static_method.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            ctxt.error("Static method types cannot be open-ended");
                            return ts.getStaticMethodType(ts.getVoidType(), List.of());
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() != Variance.INVARIANT) {
                                ctxt.error("Static method types cannot be open-ended");
                                return ts.getStaticMethodType(ts.getVoidType(), List.of());
                            }
                            // looks right, get the proper pointee type
                            ReferenceTypeSignature pointeeSig = bound.getBound();
                            if (! (pointeeSig instanceof ClassTypeSignature ctsBound)) {
                                ctxt.error("Static method pointee must be a functional interface");
                                return ts.getStaticMethodType(ts.getVoidType(), List.of());
                            }
                            // todo: use context to resolve type variable bounds
                            ClassTypeDescriptor classDesc = ctsBound.asDescriptor(classCtxt);
                            final String name;
                            if (classDesc.getPackageName().isEmpty()) {
                                name = classDesc.getClassName();
                            } else {
                                name = classDesc.getPackageName() + '/' + classDesc.getClassName();
                            }
                            if (name.equals("java/lang/Object")) {
                                // special case: it's really an "any" pointer with extra front-end guards on it
                                ctxt.error("Static method types cannot be open-ended");
                                return ts.getStaticMethodType(ts.getVoidType(), List.of());
                            }
                            DefinedTypeDefinition definedType = classCtxt.findDefinedType(name);
                            if (definedType == null) {
                                ctxt.error("Static method interface type \"%s\" is missing", name);
                                return ts.getStaticMethodType(ts.getVoidType(), List.of());
                            } else {
                                if (definedType.isInterface()) {
                                    return NativeInfo.get(ctxt).getInterfaceAsStaticMethodType(definedType);
                                } else {
                                    ctxt.error("Static method interface type \"%s\" is not an interface", name);
                                    return ts.getStaticMethodType(ts.getVoidType(), List.of());
                                }
                            }
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        staticMethodType = ts.getStaticMethodType(ts.getVoidType(), List.of());
                    }
                    return staticMethodType;
                } else if (ctd.getClassName().equals(Native.INSTANCE_METHOD)) {
                    InstanceMethodType instanceMethodType;
                    // check the signature
                    TypeSystem ts = ctxt.getTypeSystem();
                    if (signature instanceof ClassTypeSignature sig) {
                        if (! sig.getIdentifier().equals(Native.INSTANCE_METHOD)) {
                            ctxt.warning("Incorrect generic signature (expected a %s but got \"%s\")", instance_method.class, sig);
                            break out;
                        }
                        List<TypeArgument> args = sig.getTypeArguments();
                        if (args.size() != 2) {
                            ctxt.warning("Incorrect number of generic signature arguments (expected a %s but got \"%s\")", instance_method.class, sig);
                            break out;
                        }
                        TypeArgument typeArgument = args.get(0);
                        if (typeArgument instanceof AnyTypeArgument) {
                            ctxt.error("Instance method types cannot be open-ended");
                            return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() != Variance.INVARIANT) {
                                ctxt.error("Instance method types cannot be open-ended");
                                return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                            }
                            // looks right, get the proper pointee type
                            ReferenceTypeSignature pointeeSig = bound.getBound();
                            if (! (pointeeSig instanceof ClassTypeSignature ctsBound)) {
                                ctxt.error("Instance method pointee must be a functional interface");
                                return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                            }
                            // todo: use context to resolve type variable bounds
                            ClassTypeDescriptor classDesc = ctsBound.asDescriptor(classCtxt);
                            final String name;
                            if (classDesc.getPackageName().isEmpty()) {
                                name = classDesc.getClassName();
                            } else {
                                name = classDesc.getPackageName() + '/' + classDesc.getClassName();
                            }
                            if (name.equals("java/lang/Object")) {
                                // special case: it's really an "any" pointer with extra front-end guards on it
                                ctxt.error("Instance method types cannot be open-ended");
                                return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                            }
                            DefinedTypeDefinition definedType = classCtxt.findDefinedType(name);
                            if (definedType == null) {
                                ctxt.error("Instance method interface type \"%s\" is missing", name);
                                return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                            } else {
                                if (definedType.isInterface()) {
                                    TypeArgument receiverTypeArgument = args.get(1);
                                    if (receiverTypeArgument instanceof BoundTypeArgument bta) {
                                        if (bta.getVariance() != Variance.INVARIANT) {
                                            ctxt.error("Instance method receiver types cannot be open-ended");
                                            return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                                        }
                                        ReferenceTypeSignature rcvBound = bta.getBound();
                                        TypeDescriptor desc = rcvBound.asDescriptor(classCtxt);
                                        ValueType receiverType = classCtxt.resolveTypeFromDescriptor(desc, paramCtxt, rcvBound);
                                        return NativeInfo.get(ctxt).getInterfaceAsInstanceMethodType(definedType, receiverType);
                                    } else {
                                        ctxt.error("Instance method receiver types cannot be open-ended");
                                        return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                                    }
                                } else {
                                    ctxt.error("Instance method interface type \"%s\" is not an interface", name);
                                    return ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                                }
                            }
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        instanceMethodType = ts.getInstanceMethodType(ts.getVoidType(), ts.getVoidType(), List.of());
                    }
                    return instanceMethodType;
                }
            }
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }
}
