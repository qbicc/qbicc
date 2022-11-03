package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.ClassContext;
import org.qbicc.plugin.patcher.OnceRunTimeInitializerResolver;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.type.definition.ConstructorResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.MethodResolver;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

public class QbiccFeatureTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;
    private final ClassContext classContext;
    private boolean runtimeInitialized;
    private boolean reflectiveFields;
    private boolean reflectiveConstructors;
    private boolean reflectiveMethods;
    private InitializerResolver initResolver;
    private int initIndex;

    public QbiccFeatureTypeBuilder(final ClassContext classCtxt, DefinedTypeDefinition.Builder delegate) {
        this.delegate = delegate;
        this.classContext = classCtxt;
    }

    @Override
    public DefinedTypeDefinition.Builder getDelegate() {
        return delegate;
    }

    @Override
    public void setName(String internalName) {
        FeaturePatcher fp = FeaturePatcher.get(classContext.getCompilationContext());
        if (fp.isRuntimeInitializedClass(internalName)) {
            runtimeInitialized = true;
        }
        if (fp.hasReflectiveConstructors(internalName)) {
            reflectiveConstructors = true;
        }
        if (fp.hasReflectiveFields(internalName)) {
            reflectiveFields = true;
        }
        if (fp.hasReflectiveMethods(internalName)) {
            reflectiveMethods = true;
        }
        delegate.setName(internalName);
    }

    @Override
    public void setInitializer(InitializerResolver resolver, int index) {
        if (runtimeInitialized) {
            initResolver = new OnceRunTimeInitializerResolver(resolver);
            initIndex = index;
            delegate.setInitializer(resolver, -1);
        } else {
            delegate.setInitializer(resolver, index);
        }
    }

    @Override
    public void addConstructor(ConstructorResolver resolver, int index, MethodDescriptor descriptor) {
        if (reflectiveConstructors) {
            Delegating.super.addConstructor(new ConstructorResolver() {
                @Override
                public ConstructorElement resolveConstructor(int index, DefinedTypeDefinition enclosing, ConstructorElement.Builder builder) {
                    ConstructorElement constructorElement = resolver.resolveConstructor(index, enclosing, builder);
                    FeaturePatcher fp = FeaturePatcher.get(classContext.getCompilationContext());
                    String className = constructorElement.getEnclosingType().getInternalName();
                    if (fp.isReflectiveConstructor(className, constructorElement.getDescriptor())) {
                        ReachabilityRoots.get(classContext.getCompilationContext()).registerReflectiveConstructor(constructorElement);
                    }
                    return constructorElement;
                }
            }, index, descriptor);
        } else {
            delegate.addConstructor(resolver, index, descriptor);
        }
    }

    @Override
    public void addMethod(MethodResolver resolver, int index, String name, MethodDescriptor descriptor) {
        if (reflectiveMethods) {
            Delegating.super.addMethod(new MethodResolver() {
                @Override
                public MethodElement resolveMethod(int index, DefinedTypeDefinition enclosing, MethodElement.Builder builder) {
                    MethodElement methodElement = resolver.resolveMethod(index, enclosing, builder);
                    FeaturePatcher fp = FeaturePatcher.get(classContext.getCompilationContext());
                    String className = methodElement.getEnclosingType().getInternalName();
                    if (fp.isReflectiveMethod(className, methodElement.getName(), methodElement.getDescriptor())) {
                        ReachabilityRoots.get(classContext.getCompilationContext()).registerReflectiveMethod(methodElement);
                    }
                    return methodElement;
                }
            }, index, name, descriptor);
        } else {
            delegate.addMethod(resolver, index, name, descriptor);
        }
    }

    @Override
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        if (runtimeInitialized) {
            resolver = new RuntimeInitFieldResolver(initResolver, initIndex, resolver);
        }
        if (reflectiveFields) {
            final FieldResolver fr = resolver;
            Delegating.super.addField(new FieldResolver() {
                @Override
                public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
                    FieldElement fieldElement = fr.resolveField(index, enclosing, builder);
                    FeaturePatcher fp = FeaturePatcher.get(classContext.getCompilationContext());
                    String className = fieldElement.getEnclosingType().getInternalName();
                    if (fp.isReflectiveField(className, fieldElement.getName())) {
                        ReachabilityRoots.get(classContext.getCompilationContext()).registerReflectiveField(fieldElement);
                    }
                    return fieldElement;
                }
            }, index, name, descriptor);
        } else {
            delegate.addField(resolver, index, name, descriptor);
        }
    }

    static class RuntimeInitFieldResolver implements FieldResolver {
        private final InitializerResolver initResolver;
        private final int initIndex;
        private final FieldResolver fieldResolver;

        RuntimeInitFieldResolver(final InitializerResolver initResolver, int initIndex, FieldResolver fieldResolver) {
            this.initResolver = initResolver;
            this.initIndex = initIndex;
            this.fieldResolver = fieldResolver;
        }

        @Override
        public FieldElement resolveField(int index, DefinedTypeDefinition enclosing, FieldElement.Builder builder) {
            InitializerElement rtInit = initResolver.resolveInitializer(initIndex, enclosing, InitializerElement.builder());
            builder.setRunTimeInitializer(rtInit);
            FieldElement fieldElement = fieldResolver.resolveField(index, enclosing, builder);
            if (fieldElement.isStatic()) {
                fieldElement.setModifierFlags(ClassFile.I_ACC_RUN_TIME);
            }
            return fieldElement;
        }
    }
}
