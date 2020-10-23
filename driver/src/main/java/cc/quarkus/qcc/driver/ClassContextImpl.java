package cc.quarkus.qcc.driver;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.interpreter.JavaObject;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
final class ClassContextImpl implements ClassContext {
    private final CompilationContextImpl compilationContext;
    private final JavaObject classLoader;
    private final ConcurrentMap<String, AtomicReference<Object>> definedClasses = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeIdLiteral, DefinedTypeDefinition> classForLiteral = new ConcurrentHashMap<>();

    private static final Object LOADING = new Object();
    private static final Object NOT_FOUND = new Object();

    ClassContextImpl(final CompilationContextImpl compilationContext, final JavaObject classLoader) {
        this.compilationContext = compilationContext;
        this.classLoader = classLoader;
    }

    public JavaObject getClassLoader() {
        return classLoader;
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        BiFunction<JavaObject, String, DefinedTypeDefinition> finder = compilationContext.getFinder();
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
}
