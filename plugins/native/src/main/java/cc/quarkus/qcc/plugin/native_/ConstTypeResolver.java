package cc.quarkus.qcc.plugin.native_;

import java.util.List;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.Annotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotation;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

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

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        boolean const_ = false;
        for (TypeAnnotation typeAnnotation : visibleAnnotations) {
            Annotation annotation = typeAnnotation.getAnnotation();
            if (annotation.getDescriptor().getClassName().equals(Native.ANN_CONST)) {
                const_ = true;
                break;
            }
        }
        ValueType valueType = getDelegate().resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
        return const_ ? valueType.asConst() : valueType;
    }
}
