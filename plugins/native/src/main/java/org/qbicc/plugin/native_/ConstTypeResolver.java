package org.qbicc.plugin.native_;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 *
 */
public class ConstTypeResolver implements DescriptorTypeResolver.Delegating {
    private final ClassContext classCtxt;
    private final CompilationContext ctxt;
    private final DescriptorTypeResolver delegate;

    public ConstTypeResolver(final ClassContext classCtxt, final DescriptorTypeResolver delegate) {
        this.classCtxt = classCtxt;
        ctxt = classCtxt.getCompilationContext();
        this.delegate = delegate;
    }

    public DescriptorTypeResolver getDelegate() {
        return delegate;
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return getDelegate().resolveTypeFromDescriptor(descriptor, paramCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }
}
