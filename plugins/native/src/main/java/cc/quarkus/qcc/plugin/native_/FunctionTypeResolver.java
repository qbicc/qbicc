package cc.quarkus.qcc.plugin.native_;

import static cc.quarkus.qcc.runtime.CNative.*;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.descriptor.ClassTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.AnyTypeArgument;
import cc.quarkus.qcc.type.generic.BoundTypeArgument;
import cc.quarkus.qcc.type.generic.ClassTypeSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.ReferenceTypeSignature;
import cc.quarkus.qcc.type.generic.TypeArgument;
import cc.quarkus.qcc.type.generic.TypeSignature;
import cc.quarkus.qcc.type.generic.Variance;

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

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, TypeAnnotationList visibleAnnotations, TypeAnnotationList invisibleAnnotations) {
        out: if (descriptor instanceof ClassTypeDescriptor) {
            ClassTypeDescriptor ctd = (ClassTypeDescriptor) descriptor;
            if (ctd.getPackageName().equals(Native.NATIVE_PKG)) {
                if (ctd.getClassName().equals(Native.FUNCTION)) {
                    FunctionType functionType;
                    // check the signature
                    TypeSystem ts = ctxt.getTypeSystem();
                    if (signature instanceof ClassTypeSignature) {
                        ClassTypeSignature sig = (ClassTypeSignature) signature;
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
                            functionType = ts.getFunctionType(ts.getVoidType());
                        } else {
                            assert typeArgument instanceof BoundTypeArgument;
                            BoundTypeArgument bound = (BoundTypeArgument) typeArgument;
                            if (bound.getVariance() != Variance.INVARIANT) {
                                ctxt.error("Function types cannot be open-ended");
                            }
                            // looks right, get the proper pointee type
                            ReferenceTypeSignature pointeeSig = bound.getBound();
                            // todo: use context to resolve type variable bounds
                            TypeDescriptor pointeeDesc = pointeeSig.asDescriptor(classCtxt);
                            if (pointeeDesc instanceof ClassTypeDescriptor) {
                                ClassTypeDescriptor classDesc = (ClassTypeDescriptor) pointeeDesc;
                                final String name;
                                if (classDesc.getPackageName().isEmpty()) {
                                    name = classDesc.getClassName();
                                } else {
                                    name = classDesc.getPackageName() + '/' + classDesc.getClassName();
                                }
                                if (name.equals("java/lang/Object")) {
                                    // special case: it's really an "any" pointer with extra front-end guards on it
                                    return ts.getVoidType().asConst().getPointer();
                                }
                                DefinedTypeDefinition definedType = classCtxt.findDefinedType(name);
                                if (definedType == null) {
                                    ctxt.error("Function interface type \"%s\" is missing", name);
                                    functionType = ts.getFunctionType(ts.getVoidType());
                                } else {
                                    if (definedType.isInterface()) {
                                        return NativeInfo.get(ctxt).getTypeOfFunctionalInterface(definedType);
                                    } else {
                                        ctxt.error("Function interface type \"%s\" is not an interface", name);
                                        functionType = ts.getFunctionType(ts.getVoidType());
                                    }
                                }
                            } else {
                                ctxt.error("Function pointee must be a functional interface");
                                functionType = ts.getFunctionType(ts.getVoidType());
                            }
                        }
                    } else {
                        // todo: how can we cleanly get the location from here?
                        ctxt.warning("Generic signature type mismatch (expected a %s but got a %s)", ClassTypeSignature.class, signature.getClass());
                        functionType = ts.getFunctionType(ts.getVoidType());
                    }
                    return functionType;
                } else {
                    break out;
                }
            } else {
                break out;
            }
        }
        return getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }
}
