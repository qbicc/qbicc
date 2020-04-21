package cc.quarkus.qcc.type;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.spi.ClassFinder;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class Universe {

    public static final int ASM_VERSION = Opcodes.ASM7;

    private static final AtomicReference<Universe> INSTANCE = new AtomicReference<>();

    public static Universe instance() {
        return INSTANCE.get();
    }

    public Universe(ClassFinder classFinder) {
        this(classFinder, ForkJoinPool.commonPool());
    }

    public Universe(ClassFinder classFinder, ForkJoinPool pool) {
        this.classFinder = classFinder;
        this.pool = pool;
        INSTANCE.set(this);
    }

    public TypeDescriptor<?> findType(String name) {
        if ( name.equals("void")) {
            //return PrimitiveTypeDef.VOID;
            return TypeDescriptor.VOID;
        } else if ( name.equals("byte")) {
            return TypeDescriptor.BYTE;
        } else if ( name.equals("char")) {
            return TypeDescriptor.CHAR;
        } else if ( name.equals("short")) {
            return TypeDescriptor.SHORT;
        } else if ( name.equals("int")) {
            return TypeDescriptor.INT;
        } else if ( name.equals("long")) {
            return TypeDescriptor.LONG;
        } else if ( name.equals("boolean")) {
            return TypeDescriptor.BOOLEAN;
        } else if ( name.equals("float")) {
            return TypeDescriptor.FLOAT;
        } else if ( name.equals("double")) {
            return TypeDescriptor.DOUBLE;
        }
        return TypeDescriptor.of( findClass(name) );
    }

    public TypeDefinition findClass(String name) {
        return findClass(name, false);
    }

    public TypeDefinition findClass(String name, boolean resolve) {
        return this.objectTypes.computeIfAbsent(name, (k) -> new LazyTypeDefinition(this, name, resolve));
    }

    TypeDefinition defineClass(String name, ByteBuffer buffer) {
        ClassReader reader = null;
        try {
            reader = new ClassReader(new ByteBufferInputStream(buffer));
        } catch (IOException e) {
            return new UnresolvableClassDefinition(name);
        }
        TypeDefinitionNode node = new TypeDefinitionNode(this);
        reader.accept(node, 0);
        return node;
    }

    public void await(long timeout, TimeUnit unit) {
        this.pool.awaitQuiescence(timeout, unit);
        System.err.println("resolved classes: " + this.objectTypes.size());
    }

    public ClassFinder getClassFinder() {
        return this.classFinder;
    }

    public ForkJoinPool getPool() {
        return this.pool;
    }

    private final ClassFinder classFinder;

    private final ForkJoinPool pool;

    private final ConcurrentHashMap<String, TypeDefinition> objectTypes = new ConcurrentHashMap<>();

}
