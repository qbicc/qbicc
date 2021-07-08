package org.qbicc.plugin.native_;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;

public class InternalNativeTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

   public InternalNativeTypeResolver(ClassContext classCtxt, DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        this.ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    @Override
    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    @Override
    public ValueType resolveTypeFromClassName(String packageName, String internalName) {
        DefinedTypeDefinition definedType = classCtxt.findDefinedType(packageName + "/" + internalName);
        if (definedType == null) {
            return delegate.resolveTypeFromClassName(packageName, internalName);
        }
        NativeInfo nativeInfo = NativeInfo.get(ctxt);
        ValueType valueType = nativeInfo.resolveInternalNativeType(definedType);
        return valueType == null ? delegate.resolveTypeFromClassName(packageName, internalName) : valueType;
    }
}
