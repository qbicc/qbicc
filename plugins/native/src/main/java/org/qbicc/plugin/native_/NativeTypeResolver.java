package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;

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
        if (packageName.equals(Native.NATIVE_PKG)) {
            if (internalName.equals(Native.TYPE_ID)) {
                return classCtxt.findDefinedType("java/lang/Object").load().getClassType().getReference().getTypeType();
            } else if (internalName.equals(Native.VOID)) {
                return ctxt.getTypeSystem().getVoidType();
            } else if (internalName.equals(Native.PTR)) {
                return ctxt.getTypeSystem().getVoidType().getPointer();
            }
        }
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        DefinedTypeDefinition definedType = classCtxt.findDefinedType(packageName + "/" + internalName);
        if (definedType == null) {
            return delegate.resolveTypeFromClassName(packageName, internalName);
        }
        ValueType valueType = nativeInfo.resolveNativeType(definedType);
        return valueType == null ? delegate.resolveTypeFromClassName(packageName, internalName) : valueType;
    }
}
