package cc.quarkus.qcc.driver;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;
import cc.quarkus.qcc.type.generic.MethodSignature;
import cc.quarkus.qcc.type.generic.ParameterizedSignature;
import cc.quarkus.qcc.type.generic.TypeSignature;

/**
 *
 */
final class ClassContextImpl implements ClassContext {
    private final CompilationContextImpl compilationContext;
    private final VmObject classLoader;
    private final DescriptorTypeResolver descriptorTypeResolver;
    private final ConcurrentMap<String, AtomicReference<Object>> definedClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeIdLiteral, DefinedTypeDefinition> classForLiteral = new ConcurrentHashMap<>();

    private static final Object LOADING = new Object();
    private static final Object NOT_FOUND = new Object();

    ClassContextImpl(final CompilationContextImpl compilationContext, final VmObject classLoader) {
        this.compilationContext = compilationContext;
        this.classLoader = classLoader;
        DescriptorTypeResolver descriptorTypeResolver = new BasicDescriptorTypeResolver(this);
        for (BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver> factory : compilationContext.resolverFactories) {
            descriptorTypeResolver = factory.apply(this, descriptorTypeResolver);
        }
        this.descriptorTypeResolver = descriptorTypeResolver;
    }

    public CompilationContext getCompilationContext() {
        return compilationContext;
    }

    public VmObject getClassLoader() {
        return classLoader;
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        BiFunction<VmObject, String, DefinedTypeDefinition> finder = compilationContext.getFinder();
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
                definition = finder.apply(classLoader, typeName);
                ref.set(definition == null ? NOT_FOUND : definition);
            }
            return definition;
        }
    }

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
        return classForLiteral.get(typeId);
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

    public void registerClassLiteral(final ClassTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {
        classForLiteral.put(literal, typeDef);
    }

    public void registerInterfaceLiteral(final InterfaceTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {
        classForLiteral.put(literal, typeDef);
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

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return descriptorTypeResolver.resolveTypeFromDescriptor(descriptor, typeParamCtxt, signature, visibleAnnotations, invisibleAnnotations);
    }

    public FunctionType resolveTypeFromMethodDescriptor(MethodDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, MethodSignature signature, final TypeAnnotationList returnTypeVisible, List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations) {
        return descriptorTypeResolver.resolveTypeFromMethodDescriptor(descriptor, typeParamCtxt, signature, returnTypeVisible, visibleAnnotations, returnTypeInvisible, invisibleAnnotations);
    }

}
