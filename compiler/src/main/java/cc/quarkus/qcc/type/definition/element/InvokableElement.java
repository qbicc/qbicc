package cc.quarkus.qcc.type.definition.element;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.MethodHandle;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import io.smallrye.common.constraint.Assert;

/**
 * An element which is executable and can be directly invoked.
 */
public abstract class InvokableElement extends AnnotatedElement implements ExecutableElement {
    private final MethodDescriptor descriptor;
    private final MethodSignature signature;
    private final TypeAnnotationList returnVisibleTypeAnnotations;
    private final TypeAnnotationList returnInvisibleTypeAnnotations;
    private final List<ParameterElement> parameters;
    private final MethodHandle methodBody;
    private final DefinedTypeDefinition enclosingType;
    private List<TypeAnnotationList> parameterVisibleTypeAnnotations;
    private List<TypeAnnotationList> parameterInvisibleTypeAnnotations;
    private FunctionType type;

    InvokableElement() {
        super();
        this.descriptor = null;
        this.signature = null;
        this.parameters = null;
        this.returnVisibleTypeAnnotations = null;
        this.returnInvisibleTypeAnnotations = null;
        this.methodBody = null;
        this.enclosingType = null;
    }

    InvokableElement(Builder builder) {
        super(builder);
        this.descriptor = builder.descriptor;
        this.signature = builder.signature;
        this.parameters = builder.parameters;
        this.returnVisibleTypeAnnotations = builder.returnVisibleTypeAnnotations;
        this.returnInvisibleTypeAnnotations = builder.returnInvisibleTypeAnnotations;
        this.methodBody = builder.methodBody;
        this.enclosingType = Assert.checkNotNullParam("builder.enclosingType", builder.enclosingType);
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public MethodSignature getSignature() {
        return signature;
    }

    public boolean hasMethodBody() {
        return methodBody != null;
    }

    public MethodHandle getMethodBody() {
        return methodBody;
    }

    public DefinedTypeDefinition getEnclosingType() {
        return enclosingType;
    }

    public FunctionType getType(final ClassContext classContext, final List<ParameterizedSignature> signatureContext) {
        FunctionType type = this.type;
        if (type == null) {
            this.type = type = classContext.resolveTypeFromMethodDescriptor(
                descriptor,
                signatureContext,
                signature,
                returnVisibleTypeAnnotations,
                getParameterVisibleTypeAnnotations(),
                returnInvisibleTypeAnnotations,
                getParameterInvisibleTypeAnnotations()
            );
        }
        return type;
    }

    public List<ParameterElement> getParameters() {
        return parameters;
    }

    public List<TypeAnnotationList> getParameterVisibleTypeAnnotations() {
        List<TypeAnnotationList> annotations = this.parameterVisibleTypeAnnotations;
        if (annotations == null) {
            assert parameters != null;
            annotations = new ArrayList<>(parameters.size());
            for (ParameterElement parameter : parameters) {
                annotations.add(parameter.getVisibleTypeAnnotations());
            }
            parameterVisibleTypeAnnotations = annotations;
        }
        return annotations;
    }

    public List<TypeAnnotationList> getParameterInvisibleTypeAnnotations() {
        List<TypeAnnotationList> annotations = this.parameterInvisibleTypeAnnotations;
        if (annotations == null) {
            assert parameters != null;
            annotations = new ArrayList<>(parameters.size());
            for (ParameterElement parameter : parameters) {
                annotations.add(parameter.getInvisibleTypeAnnotations());
            }
            parameterInvisibleTypeAnnotations = annotations;
        }
        return annotations;
    }

    public static abstract class Builder extends AnnotatedElement.Builder implements ExecutableElement.Builder {
        List<ParameterElement> parameters = List.of();
        MethodDescriptor descriptor = MethodDescriptor.VOID_METHOD_DESCRIPTOR;
        MethodSignature signature = MethodSignature.VOID_METHOD_SIGNATURE;
        TypeAnnotationList returnVisibleTypeAnnotations = TypeAnnotationList.empty();
        TypeAnnotationList returnInvisibleTypeAnnotations = TypeAnnotationList.empty();
        MethodHandle methodBody;
        DefinedTypeDefinition enclosingType;

        Builder() {}

        public void setDescriptor(MethodDescriptor descriptor) {
            this.descriptor = Assert.checkNotNullParam("descriptor", descriptor);
        }

        public void setSignature(final MethodSignature signature) {
            this.signature = Assert.checkNotNullParam("signature", signature);
        }

        public void setParameters(final List<ParameterElement> parameters) {
            this.parameters = Assert.checkNotNullParam("parameters", parameters);
        }

        public void setReturnVisibleTypeAnnotations(final TypeAnnotationList returnVisibleTypeAnnotations) {
            this.returnVisibleTypeAnnotations = Assert.checkNotNullParam("returnVisibleTypeAnnotations", returnVisibleTypeAnnotations);
        }

        public void setReturnInvisibleTypeAnnotations(final TypeAnnotationList returnInvisibleTypeAnnotations) {
            this.returnInvisibleTypeAnnotations = Assert.checkNotNullParam("returnInvisibleTypeAnnotations", returnInvisibleTypeAnnotations);
        }

        public void setMethodBody(final MethodHandle methodHandle) {
            this.methodBody = methodHandle;
        }

        public void setEnclosingType(final DefinedTypeDefinition enclosingType) {
            this.enclosingType = Assert.checkNotNullParam("enclosingType", enclosingType);
        }

        public abstract InvokableElement build();
    }
}
