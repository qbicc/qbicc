package org.qbicc.driver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefineFailedException;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.TypeParameterContext;
import org.qbicc.type.generic.TypeSignature;

/**
 *
 */
final class ClassContextImpl implements ClassContext {
    private final CompilationContextImpl compilationContext;
    private final VmClassLoader classLoader;
    private final DescriptorTypeResolver descriptorTypeResolver;
    private final ConcurrentMap<String, AtomicReference<Object>> definedClasses = new ConcurrentHashMap<>();
    private final BiFunction<ClassContext, String, DefinedTypeDefinition> finder;
    private final BiFunction<ClassContext, String, byte[]> resourceFinder;
    private final BiFunction<ClassContext, String, List<byte[]>> resourcesFinder;

    private static final Object LOADING = new Object();
    private static final Object NOT_FOUND = new Object();

    ClassContextImpl(final CompilationContextImpl compilationContext, final VmClassLoader classLoader, BiFunction<ClassContext, String, DefinedTypeDefinition> finder, BiFunction<ClassContext, String, byte[]> resourceFinder, BiFunction<ClassContext, String, List<byte[]>> resourcesFinder) {
        this.compilationContext = compilationContext;
        this.classLoader = classLoader;
        this.finder = finder;
        this.resourceFinder = resourceFinder;
        this.resourcesFinder = resourcesFinder;
        DescriptorTypeResolver descriptorTypeResolver = new BasicDescriptorTypeResolver(this);
        for (BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver> factory : compilationContext.resolverFactories) {
            descriptorTypeResolver = factory.apply(this, descriptorTypeResolver);
        }
        this.descriptorTypeResolver = descriptorTypeResolver;
    }

    public CompilationContext getCompilationContext() {
        return compilationContext;
    }

    public VmClassLoader getClassLoader() {
        return classLoader;
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        AtomicReference<Object> ref = definedClasses.get(typeName);
        Object val;
        DefinedTypeDefinition definition;
        for (;;) {
            if (ref != null) {
                val = ref.get();
                if (val == LOADING) {
                    synchronized (ref) {
                        val = ref.get();
                    }
                }
                return val == NOT_FOUND ? null : (DefinedTypeDefinition) val;
            }
            // not found
            ref = new AtomicReference<>(LOADING);
            synchronized (ref) {
                AtomicReference<Object> appearing = definedClasses.putIfAbsent(typeName, ref);
                if (appearing != null) {
                    ref = appearing;
                    continue;
                }
                definition = finder.apply(this, typeName);
                ref.set(definition == null ? NOT_FOUND : definition);
            }
            return definition;
        }
    }

    public byte[] getResource(String resourceName) {
        return resourceFinder.apply(this, resourceName);
    }

    public List<byte[]> getResources(String resourceName) {
        return resourcesFinder.apply(this, resourceName);
    }

    public boolean isBootstrap() {
        return this == compilationContext.getBootstrapClassContext();
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        return compilationContext.deduplicate(buffer, offset, length);
    }

    public String deduplicate(final String original) {
        return compilationContext.deduplicate(original);
    }

    public TypeSystem getTypeSystem() {
        return compilationContext.getTypeSystem();
    }

    public LiteralFactory getLiteralFactory() {
        return compilationContext.getLiteralFactory();
    }

    public BasicBlockBuilder newBasicBlockBuilder(final ExecutableElement element) {
        return compilationContext.getBlockFactory().apply(compilationContext, element);
    }

    public void defineClass(final String name, final DefinedTypeDefinition definition) {
        AtomicReference<Object> ref = definedClasses.get(name);
        if (ref == null) {
            ref = new AtomicReference<>(definition);
            ref = definedClasses.putIfAbsent(name, ref);
            if (ref == null) {
                // done
                return;
            }
        }
        if (Thread.holdsLock(ref)) {
            // already being loaded by our thread
            assert ref.get() == LOADING;
            return;
        }
        throw new DefineFailedException("Duplicated class named " + name);
    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        return descriptorTypeResolver.resolveTypeFromClassName(packageName, internalName);
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        return descriptorTypeResolver.resolveTypeFromDescriptor(descriptor, paramCtxt, signature);
    }

    public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        return descriptorTypeResolver.resolveArrayObjectTypeFromDescriptor(descriptor, paramCtxt, signature);
    }

    @Override
    public DefinedTypeDefinition.Builder newTypeBuilder() {
        DefinedTypeDefinition.Builder builder = DefinedTypeDefinition.Builder.basic();
        for (BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder> factory : compilationContext.getTypeBuilderFactories()) {
            builder = factory.apply(this, builder);
        }
        return builder;
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature) {
        return descriptorTypeResolver.resolveTypeFromMethodDescriptor(descriptor, paramCtxt, signature);
    }
}
