package org.qbicc.type.definition.element;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import org.qbicc.runtime.ExtModifier;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.annotation.Annotation;
import org.qbicc.type.annotation.EnumConstantAnnotationValue;
import org.qbicc.type.annotation.IntAnnotationValue;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.type.definition.MethodBody;
import org.qbicc.type.definition.MethodBodyFactory;
import org.qbicc.type.definition.ParameterResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.MethodSignature;
import org.qbicc.type.generic.TypeParameter;
import org.qbicc.type.generic.TypeParameterContext;
import io.smallrye.common.constraint.Assert;

/**
 * An element which is executable and can be directly invoked.
 */
public abstract class InvokableElement extends AnnotatedElement implements ExecutableElement, TypeParameterContext {
    static final int[] NO_INTS = new int[0];
    static final ParameterResolver[] NO_PARAMETER_RESOLVERS = new ParameterResolver[0];
    static final String[] NO_STRINGS = new String[0];
    static final TypeDescriptor[] NO_DESCS = new TypeDescriptor[0];

    private static final VarHandle typeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "type", VarHandle.class, InvokableElement.class, InvokableType.class);

    private final MethodDescriptor descriptor;
    private final MethodSignature signature;
    private final TypeAnnotationList visibleTypeAnnotations;
    private final TypeAnnotationList invisibleTypeAnnotations;
    private final ParameterResolver[] parameterResolvers;
    private final int[] parameterResolverIndexes;
    private final String[] parameterNames;
    private final TypeDescriptor[] parameterDescs;
    private final SafePointBehavior safePointBehavior;
    private final int safePointSetBits;
    private final int safePointClearBits;
    private List<TypeAnnotationList> parameterVisibleTypeAnnotations;
    private List<TypeAnnotationList> parameterInvisibleTypeAnnotations;
    private volatile InvokableType type;
    final MethodBodyFactory methodBodyFactory;
    final int methodBodyFactoryIndex;
    volatile MethodBody previousMethodBody;
    volatile MethodBody methodBody;
    volatile List<ParameterElement> parameters;
    final int minimumLineNumber;
    final int maximumLineNumber;
    boolean inProgress;

    InvokableElement() {
        super();
        this.descriptor = null;
        this.signature = null;
        this.parameterResolvers = NO_PARAMETER_RESOLVERS;
        this.parameterResolverIndexes = NO_INTS;
        this.parameterNames = NO_STRINGS;
        this.parameterDescs = NO_DESCS;
        this.visibleTypeAnnotations = null;
        this.invisibleTypeAnnotations = null;
        this.methodBodyFactory = null;
        this.methodBodyFactoryIndex = 0;
        this.minimumLineNumber = 1;
        this.maximumLineNumber = 1;
        this.safePointBehavior = SafePointBehavior.FORBIDDEN;
        this.safePointSetBits = 0;
        this.safePointClearBits = 0;
    }

    InvokableElement(BuilderImpl builder) {
        super(builder);
        this.descriptor = builder.descriptor;
        this.signature = builder.signature;
        int parameterCount = builder.parameterCount;
        this.parameterResolvers = Arrays.copyOf(builder.parameterResolvers, parameterCount);
        this.parameterResolverIndexes = Arrays.copyOf(builder.parameterResolverIndexes, parameterCount);
        this.parameterNames = Arrays.copyOf(builder.parameterNames, parameterCount);
        this.parameterDescs = Arrays.copyOf(builder.parameterDescs, parameterCount);
        this.visibleTypeAnnotations = builder.visibleTypeAnnotations;
        this.invisibleTypeAnnotations = builder.invisibleTypeAnnotations;
        this.methodBodyFactory = builder.methodBodyFactory;
        this.methodBodyFactoryIndex = builder.methodBodyFactoryIndex;
        this.minimumLineNumber = builder.minimumLineNumber;
        this.maximumLineNumber = builder.maximumLineNumber;
        this.type = builder.type;
        this.safePointBehavior = builder.safePointBehavior;
        this.safePointSetBits = builder.safePointSetBits;
        this.safePointClearBits = builder.safePointClearBits;
    }

    public boolean hasMethodBodyFactory() {
        return methodBodyFactory != null;
    }

    public boolean hasMethodBody() {
        return methodBody != null;
    }

    public MethodBodyFactory getMethodBodyFactory() {
        return methodBodyFactory;
    }

    @Override
    public TypeParameterContext getTypeParameterContext() {
        return this;
    }

    public int getMethodBodyFactoryIndex() {
        return methodBodyFactoryIndex;
    }

    public boolean isVarargs() {
        return hasAllModifiersOf(ClassFile.ACC_VARARGS);
    }

    public MethodBody getPreviousMethodBody() {
        return previousMethodBody;
    }

    public MethodBody getMethodBody() {
        MethodBody methodBody = this.methodBody;
        if (methodBody == null) {
            throw new IllegalStateException("No method body is present on this element");
        }
        return methodBody;
    }

    public boolean tryCreateMethodBody() {
        MethodBody methodBody = this.methodBody;
        if (methodBody == null) {
            MethodBodyFactory factory = this.methodBodyFactory;
            if (factory != null) {
                synchronized (this) {
                    methodBody = this.methodBody;
                    if (methodBody == null) {
                        if (inProgress) {
                            return true;
                        }
                        inProgress = true;
                        try {
                            this.methodBody = previousMethodBody = factory.createMethodBody(methodBodyFactoryIndex, this);
                        } catch (IllegalStateException e) {
                            getEnclosingType().getContext().getCompilationContext().warning("Failed to create body for "+this+": "+e.getMessage());
                            return false;
                        } finally {
                            inProgress = false;
                        }
                    }
                }
            } else {
                return false;
            }
        }
        return true;
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

    public InvokableType getType() {
        InvokableType type = this.type;
        if (type == null) {
            InvokableType appearing = (InvokableType) typeHandle.compareAndExchange(this, null, type = computeType());
            if (appearing != null) {
                return appearing;
            }
        }
        return type;
    }

    abstract InvokableType computeType();

    public int getMinimumLineNumber() {
        return minimumLineNumber;
    }

    public int getMaximumLineNumber() {
        return maximumLineNumber;
    }

    @Override
    public SafePointBehavior safePointBehavior() {
        return safePointBehavior;
    }

    @Override
    public int safePointSetBits() {
        return safePointSetBits;
    }

    @Override
    public int safePointClearBits() {
        return safePointClearBits;
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
        List<ParameterElement> parameters = this.parameters;
        if (parameters == null) {
            synchronized (this) {
                parameters = this.parameters;
                if (parameters == null) {
                    int cnt = parameterResolvers.length;
                    ParameterElement[] array = new ParameterElement[cnt];
                    for (int i = 0; i < cnt; i ++) {
                        array[i] = parameterResolvers[i].resolveParameter(this, parameterResolverIndexes[i], ParameterElement.builder(
                            parameterNames[i],
                            parameterDescs[i],
                            i
                        ));
                    }
                    parameters = this.parameters = List.of(array);
                }
            }
        }
        return parameters;
    }

    public TypeAnnotationList getVisibleTypeAnnotations() {
        return this.visibleTypeAnnotations;
    }

    public TypeAnnotationList getInvisibleTypeAnnotations() {
        return this.invisibleTypeAnnotations;
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

    public interface Builder extends AnnotatedElement.Builder, ExecutableElement.Builder {
        MethodDescriptor getDescriptor();

        void setSignature(final MethodSignature signature);

        void addParameter(ParameterResolver resolver, int index, String name, TypeDescriptor descriptor);

        @Deprecated
        void setParameters(final List<ParameterElement> parameters);

        void setVisibleTypeAnnotations(final TypeAnnotationList returnVisibleTypeAnnotations);

        void setInvisibleTypeAnnotations(final TypeAnnotationList returnInvisibleTypeAnnotations);

        void setMethodBodyFactory(final MethodBodyFactory factory, final int index);

        void setMinimumLineNumber(int minimumLineNumber);

        void setMaximumLineNumber(int maximumLineNumber);

        void setSafePointBehavior(SafePointBehavior behavior);

        void setSafePointSetBits(int bits);

        void setSafePointClearBits(int bits);

        InvokableElement build();

        interface Delegating extends AnnotatedElement.Builder.Delegating, ExecutableElement.Builder.Delegating, Builder {
            @Override
            Builder getDelegate();

            @Override
            default MethodDescriptor getDescriptor() {
                return getDelegate().getDescriptor();
            }

            @Override
            default void setSignature(final MethodSignature signature) {
                getDelegate().setSignature(signature);
            }

            @Override
            default void addParameter(ParameterResolver resolver, int index, String name, TypeDescriptor descriptor) {
                getDelegate().addParameter(resolver, index, name, descriptor);
            }

            @Override
            default void setParameters(final List<ParameterElement> parameters) {
                getDelegate().setParameters(parameters);
            }

            @Override
            default void setVisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setVisibleTypeAnnotations(annotations);
            }

            @Override
            default void setInvisibleTypeAnnotations(final TypeAnnotationList annotations) {
                getDelegate().setInvisibleTypeAnnotations(annotations);
            }

            @Override
            default void setMinimumLineNumber(int minimumLineNumber) {
                getDelegate().setMinimumLineNumber(minimumLineNumber);
            }

            @Override
            default void setMaximumLineNumber(int maximumLineNumber) {
                getDelegate().setMaximumLineNumber(maximumLineNumber);
            }

            @Override
            default void setMethodBodyFactory(MethodBodyFactory factory, int index) {
                getDelegate().setMethodBodyFactory(factory, index);
            }

            @Override
            default void setSafePointBehavior(SafePointBehavior behavior) {
                getDelegate().setSafePointBehavior(behavior);
            }

            @Override
            default void setSafePointSetBits(int bits) {
                getDelegate().setSafePointSetBits(bits);
            }

            @Override
            default void setSafePointClearBits(int bits) {
                getDelegate().setSafePointClearBits(bits);
            }

            @Override
            default InvokableElement build() {
                return getDelegate().build();
            }
        }
    }

    static abstract class BuilderImpl extends AnnotatedElement.BuilderImpl implements Builder {
        final MethodDescriptor descriptor;
        ParameterResolver[] parameterResolvers = NO_PARAMETER_RESOLVERS;
        int[] parameterResolverIndexes = NO_INTS;
        String[] parameterNames = NO_STRINGS;
        TypeDescriptor[] parameterDescs = NO_DESCS;
        int parameterCount;
        MethodSignature signature = MethodSignature.VOID_METHOD_SIGNATURE;
        TypeAnnotationList visibleTypeAnnotations = TypeAnnotationList.empty();
        TypeAnnotationList invisibleTypeAnnotations = TypeAnnotationList.empty();
        MethodBodyFactory methodBodyFactory;
        int methodBodyFactoryIndex;
        int minimumLineNumber = 1;
        int maximumLineNumber = 1;
        FunctionType type;
        SafePointBehavior safePointBehavior = SafePointBehavior.POLLING;
        int safePointSetBits;
        int safePointClearBits;

        BuilderImpl(MethodDescriptor descriptor, int index) {
            super(index);
            this.descriptor = descriptor;
        }

        public MethodDescriptor getDescriptor() {
            return descriptor;
        }

        public void setSignature(final MethodSignature signature) {
            this.signature = Assert.checkNotNullParam("signature", signature);
        }

        public void addParameter(ParameterResolver resolver, int index, String name, TypeDescriptor descriptor) {
            int parameterCount = this.parameterCount;
            if (parameterResolvers.length == parameterCount) {
                int newSize = parameterCount + 8;
                parameterResolvers = Arrays.copyOf(parameterResolvers, newSize);
                parameterResolverIndexes = Arrays.copyOf(parameterResolverIndexes, newSize);
                parameterNames = Arrays.copyOf(parameterNames, newSize);
                parameterDescs = Arrays.copyOf(parameterDescs, newSize);
            }
            parameterResolvers[parameterCount] = resolver;
            parameterResolverIndexes[parameterCount] = index;
            parameterNames[parameterCount] = name;
            parameterDescs[parameterCount] = descriptor;
            this.parameterCount = parameterCount + 1;
        }

        @Deprecated
        public void setParameters(final List<ParameterElement> parameters) {
            // replace the whole list
            if (parameters.isEmpty()) {
                parameterResolvers = NO_PARAMETER_RESOLVERS;
                parameterResolverIndexes = NO_INTS;
                parameterNames = NO_STRINGS;
                parameterDescs = NO_DESCS;
                parameterCount = 0;
            } else {
                int parameterCount = parameters.size();
                ParameterResolver[] resolvers = new ParameterResolver[parameterCount];
                int[] indexes = new int[parameterCount];
                String[] names = new String[parameterCount];
                TypeDescriptor[] descs = new TypeDescriptor[parameterCount];
                ParameterResolver parameterResolver = (methodElement, idxCopy, builder) -> parameters.get(idxCopy);
                for (int i = 0; i < parameterCount; i ++) {
                    indexes[i] = i;
                    resolvers[i] = parameterResolver;
                    names[i] = parameters.get(i).getName();
                    descs[i] = parameters.get(i).getTypeDescriptor();
                }
                parameterResolvers = resolvers;
                parameterResolverIndexes = indexes;
                parameterNames = names;
                parameterDescs = descs;
                this.parameterCount = parameterCount;
            }
        }

        public void setVisibleTypeAnnotations(final TypeAnnotationList visibleTypeAnnotations) {
            this.visibleTypeAnnotations = Assert.checkNotNullParam("visibleTypeAnnotations", visibleTypeAnnotations);
        }

        public void setInvisibleTypeAnnotations(final TypeAnnotationList invisibleTypeAnnotations) {
            this.invisibleTypeAnnotations = Assert.checkNotNullParam("invisibleTypeAnnotations", invisibleTypeAnnotations);
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

        public void setSafePointBehavior(SafePointBehavior safePointBehavior) {
            this.safePointBehavior = safePointBehavior;
        }

        public void setSafePointSetBits(int safePointSetBits) {
            this.safePointSetBits = safePointSetBits;
        }

        public void setSafePointClearBits(int safePointClearBits) {
            this.safePointClearBits = safePointClearBits;
        }

        @Override
        public void addVisibleAnnotations(List<Annotation> annotations) {
            super.addVisibleAnnotations(annotations);
            // process meaningful annotations
            for (Annotation annotation : annotations) {
                if (annotation.getDescriptor().packageAndClassNameEquals("jdk/internal/reflect", "CallerSensitive")) {
                    addModifiers(ExtModifier.I_ACC_CALLER_SENSITIVE);
                }
            }
        }

        @Override
        public void addInvisibleAnnotations(List<Annotation> annotations) {
            super.addInvisibleAnnotations(annotations);
            // process meaningful annotations
            for (Annotation annotation : annotations) {
                if (annotation.getDescriptor().packageAndClassNameEquals("org/qbicc/runtime", "SafePoint")) {
                    // assume default
                    SafePointBehavior behavior = SafePointBehavior.ENTER;
                    EnumConstantAnnotationValue value = (EnumConstantAnnotationValue) annotation.getValue("value");
                    if (value != null) {
                        behavior = SafePointBehavior.valueOf(value.getValueName());
                    }
                    setSafePointBehavior(behavior);
                    IntAnnotationValue setBits = (IntAnnotationValue) annotation.getValue("setBits");
                    if (setBits != null) {
                        setSafePointSetBits(setBits.intValue());
                    }
                    IntAnnotationValue clearBits = (IntAnnotationValue) annotation.getValue("clearBits");
                    if (clearBits != null) {
                        setSafePointClearBits(clearBits.intValue());
                    }
                }
            }
        }

        void setType(FunctionType type) {
            this.type = Assert.checkNotNullParam("type", type);
        }

        public abstract InvokableElement build();
    }
}
