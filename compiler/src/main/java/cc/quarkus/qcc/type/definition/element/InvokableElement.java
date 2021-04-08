package cc.quarkus.qcc.type.definition.element;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.MethodBodyFactory;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.TypeParameter;
import cc.quarkus.qcc.type.generic.TypeParameterContext;
import io.smallrye.common.constraint.Assert;

/**
 * An element which is executable and can be directly invoked.
 */
public abstract class InvokableElement extends AnnotatedElement implements ExecutableElement, TypeParameterContext {
    private final MethodDescriptor descriptor;
    private final MethodSignature signature;
    private final TypeAnnotationList returnVisibleTypeAnnotations;
    private final TypeAnnotationList returnInvisibleTypeAnnotations;
    private final List<ParameterElement> parameters;
    private List<TypeAnnotationList> parameterVisibleTypeAnnotations;
    private List<TypeAnnotationList> parameterInvisibleTypeAnnotations;
    private FunctionType type;
    final MethodBodyFactory methodBodyFactory;
    final int methodBodyFactoryIndex;
    volatile MethodBody previousMethodBody;
    volatile MethodBody methodBody;
    final int minimumLineNumber;
    final int maximumLineNumber;

    InvokableElement() {
        super();
        this.descriptor = null;
        this.signature = null;
        this.parameters = null;
        this.returnVisibleTypeAnnotations = null;
        this.returnInvisibleTypeAnnotations = null;
        this.methodBodyFactory = null;
        this.methodBodyFactoryIndex = 0;
        this.minimumLineNumber = 1;
        this.maximumLineNumber = 1;
    }

    InvokableElement(Builder builder) {
        super(builder);
        this.descriptor = builder.descriptor;
        this.signature = builder.signature;
        this.parameters = builder.parameters;
        this.returnVisibleTypeAnnotations = builder.returnVisibleTypeAnnotations;
        this.returnInvisibleTypeAnnotations = builder.returnInvisibleTypeAnnotations;
        this.methodBodyFactory = builder.methodBodyFactory;
        this.methodBodyFactoryIndex = builder.methodBodyFactoryIndex;
        this.minimumLineNumber = builder.minimumLineNumber;
        this.maximumLineNumber = builder.maximumLineNumber;
        this.type = builder.type;
    }

    public boolean hasMethodBody() {
        return methodBodyFactory != null;
    }

    public boolean isVarargs() {
        return hasAllModifiersOf(ClassFile.ACC_VARARGS);
    }

    public MethodBody getPreviousMethodBody() {
        return previousMethodBody;
    }

    public MethodBody getMethodBody() {
        return methodBody;
    }

    public MethodBody getOrCreateMethodBody() {
        MethodBody methodBody = this.methodBody;
        if (methodBody == null) {
            MethodBodyFactory factory = this.methodBodyFactory;
            if (factory != null) {
                synchronized (this) {
                    methodBody = this.methodBody;
                    if (methodBody == null) {
                        this.methodBody = previousMethodBody = methodBody = factory.createMethodBody(methodBodyFactoryIndex, this);
                    }
                }
            }
        }
        return methodBody;
    }

    public void replaceMethodBody(final MethodBody replacement) {
        MethodBody existing = this.methodBody;
        if (existing != null) {
            previousMethodBody = existing;
        }
        this.methodBody = replacement;
    }

    public MethodDescriptor getDescriptor() {
        return descriptor;
    }

    public MethodSignature getSignature() {
        return signature;
    }

    public FunctionType getType() {
        ClassContext classContext = getEnclosingType().getContext();
        FunctionType type = this.type;
        if (type == null) {
            this.type = type = classContext.resolveMethodFunctionType(
                descriptor,
                this,
                signature,
                returnVisibleTypeAnnotations,
                getParameterVisibleTypeAnnotations(),
                returnInvisibleTypeAnnotations,
                getParameterInvisibleTypeAnnotations()
            );
        }
        return type;
    }

    public int getMinimumLineNumber() {
        return minimumLineNumber;
    }

    public int getMaximumLineNumber() {
        return maximumLineNumber;
    }

    @Override
    public TypeParameter resolveTypeParameter(String parameterName) throws NoSuchElementException {
        TypeParameter parameter = getSignature().getTypeParameter(parameterName);
        if (parameter == null) {
            return getEnclosingType().resolveTypeParameter(parameterName);
        }
        return parameter;
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
        MethodBodyFactory methodBodyFactory;
        int methodBodyFactoryIndex;
        int minimumLineNumber = 1;
        int maximumLineNumber = 1;
        FunctionType type;

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

        public void setMethodBodyFactory(final MethodBodyFactory factory, final int index) {
            this.methodBodyFactory = Assert.checkNotNullParam("factory", factory);
            this.methodBodyFactoryIndex = index;
        }

        public void setMinimumLineNumber(int minimumLineNumber) {
            this.minimumLineNumber = minimumLineNumber;
        }

        public void setMaximumLineNumber(int maximumLineNumber) {
            this.maximumLineNumber = maximumLineNumber;
        }

        void setType(FunctionType type) {
            this.type = Assert.checkNotNullParam("type", type);
        }

        public abstract InvokableElement build();
    }
}
