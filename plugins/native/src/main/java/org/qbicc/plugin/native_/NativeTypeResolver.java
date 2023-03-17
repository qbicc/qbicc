package org.qbicc.plugin.native_;

import java.util.List;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.plugin.coreclasses.HeaderBits;
import org.qbicc.type.ObjectType;
import org.qbicc.type.ReferenceType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.BoundTypeArgument;
import org.qbicc.type.generic.ClassTypeSignature;
import org.qbicc.type.generic.NestedClassTypeSignature;
import org.qbicc.type.generic.ReferenceTypeSignature;
import org.qbicc.type.generic.TopLevelClassTypeSignature;
import org.qbicc.type.generic.TypeArgument;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;
import org.qbicc.type.generic.Variance;

/**
 * This type resolver is responsible for translating Java reference types such as {@code CNative.c_int} into the
 * corresponding {@code ValueType} in field, method, and constructor declarations.
 */
public class NativeTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public NativeTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        String rewrittenName = internalName.endsWith("$_native") ? internalName.substring(0, internalName.length() - "$_native".length()) : internalName;
        if (packageName.equals(Native.NATIVE_PKG)) {
            if (rewrittenName.equals(Native.TYPE_ID)) {
                return classCtxt.findDefinedType("java/lang/Object").load().getClassType().getReference().getTypeType();
            } else if (rewrittenName.equals(Native.HEADER_TYPE)) {
                return HeaderBits.get(ctxt).getHeaderType();
            } else if (rewrittenName.equals(Native.REFERENCE)) {
                // normally we'd handle this with generics, but a raw reference is OK too
                return classCtxt.resolveTypeFromClassName("java/lang", "Object");
            } else if (rewrittenName.equals(Native.VOID)) {
                return ctxt.getTypeSystem().getVoidType();
            } else if (rewrittenName.equals(Native.OBJECT)) {
                return ctxt.getTypeSystem().getVariadicType();
            } else if (rewrittenName.equals(Native.PTR)) {
                return ctxt.getTypeSystem().getVoidType().getPointer();
            }
        }
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        DefinedTypeDefinition definedType = classCtxt.findDefinedType(packageName + "/" + rewrittenName);
        if (definedType == null) {
            return delegate.resolveTypeFromClassName(packageName, rewrittenName);
        }
        ValueType valueType = nativeInfo.resolveNativeType(definedType);
        return valueType == null ? delegate.resolveTypeFromClassName(packageName, rewrittenName) : valueType;
    }

    @Override
    public ValueType resolveTypeFromDescriptor(TypeDescriptor descriptor, TypeParameterContext paramCtxt, TypeSignature signature) {
        if (descriptor instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals(Native.NATIVE_PKG, Native.REFERENCE)) {
            // try to access the generic type
            if (signature instanceof NestedClassTypeSignature nc && nc.getIdentifier().equals("reference")) {
                // check the outer class
                if (nc.getEnclosing() instanceof TopLevelClassTypeSignature cts && cts.getPackageName().equals(Native.NATIVE_PKG) && cts.getIdentifier().equals(Native.C_NATIVE)) {
                    return getReferenceType(paramCtxt, nc);
                }
            } else if (signature instanceof TopLevelClassTypeSignature tl && tl.getPackageName().equals(Native.NATIVE_PKG) && tl.getIdentifier().equals(Native.REFERENCE)) {
                return getReferenceType(paramCtxt, tl);
            }
            // in some way it wasn't parseable
            return ((ObjectType) classCtxt.resolveTypeFromClassName("java/lang", "Object")).getReference();
        }
        return delegate.resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }

    private ReferenceType getReferenceType(final TypeParameterContext paramCtxt, final ClassTypeSignature sig) {
        // OK, now look into the type arguments
        List<TypeArgument> typeArguments = sig.getTypeArguments();
        if (typeArguments.size() == 1) {
            TypeArgument typeArgument = typeArguments.get(0);
            if (typeArgument instanceof BoundTypeArgument bta) {
                Variance variance = bta.getVariance();
                if (variance == Variance.COVARIANT || variance == Variance.INVARIANT) {
                    // either is acceptable
                    ReferenceTypeSignature bound = bta.getBound();
                    TypeDescriptor boundDesc = bound.asDescriptor(classCtxt);
                    ValueType nestedType = classCtxt.resolveTypeFromDescriptor(boundDesc, paramCtxt, bound);
                    if (nestedType instanceof ObjectType ot) {
                        return ot.getReference();
                    } else {
                        ctxt.error("Cannot convert %s into an object type", nestedType);
                    }
                }
            }
        }
        // in some way it wasn't parseable
        return ((ObjectType) classCtxt.resolveTypeFromClassName("java/lang", "Object")).getReference();
    }
}
