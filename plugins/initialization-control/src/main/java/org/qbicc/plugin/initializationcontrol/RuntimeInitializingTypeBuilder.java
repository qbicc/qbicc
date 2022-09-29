package org.qbicc.plugin.initializationcontrol;

import org.qbicc.context.ClassContext;
import org.qbicc.plugin.patcher.OnceRunTimeInitializerResolver;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.FieldResolver;
import org.qbicc.type.definition.InitializerResolver;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.descriptor.TypeDescriptor;

public class RuntimeInitializingTypeBuilder implements DefinedTypeDefinition.Builder.Delegating {
    private final DefinedTypeDefinition.Builder delegate;
    private final ClassContext classContext;
    private boolean runtimeInitialized;
    private InitializerResolver initResolver;
    private int initIndex;

    public RuntimeInitializingTypeBuilder(final ClassContext classCtxt, DefinedTypeDefinition.Builder delegate) {
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
            classContext.getCompilationContext().info("Initialization of %s deferred to runtime", internalName.replace('/', '.'));
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
    public void addField(FieldResolver resolver, int index, String name, TypeDescriptor descriptor) {
        if (runtimeInitialized) {
            resolver = new RuntimeInitFieldResolver(initResolver, initIndex, resolver);
        }
        delegate.addField(resolver, index, name, descriptor);
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
            return fieldResolver.resolveField(index, enclosing, builder);
        }
    }
}
