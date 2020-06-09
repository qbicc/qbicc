package cc.quarkus.qcc.type.universe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.spi.ClassFinder;
import cc.quarkus.qcc.type.definition.LazyTypeDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinitionNode;
import cc.quarkus.qcc.type.definition.UnresolvableClassDefinition;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

public class Universe {

    public static final int ASM_VERSION = Opcodes.ASM7;

    private static final AttachmentKey<Universe> key = new AttachmentKey<>();

    public static Universe rootUniverse() {
        return Context.requireCurrent().getAttachment(key);
    }

    public static void setRootUniverse(Universe u) {
        if (Context.requireCurrent().putAttachmentIfAbsent(key, u) != null) {
            throw new IllegalStateException("Root universe already attached to context");
        }
    }

    public Universe(ClassFinder classFinder) {
        this.classFinder = classFinder;
    }

    public TypeDefinition findClass(String name) {
        return findClass(name, false);
    }

    public TypeDefinition findClass(String name, boolean resolve) {
        TypeDefinition typeDefinition = this.objectTypes.computeIfAbsent(name, (k) -> new LazyTypeDefinition(this, name));
        if (resolve && typeDefinition instanceof LazyTypeDefinition) {
            ((LazyTypeDefinition) typeDefinition).getDelegate();
        }
        return typeDefinition;
    }

    public TypeDefinition defineClass(String name, ByteBuffer buffer) {
        ClassReader reader;
        try {
            reader = new ClassReader(new ByteBufferInputStream(buffer));
        } catch (IOException e) {
            return new UnresolvableClassDefinition(name);
        }
        TypeDefinitionNode node = new TypeDefinitionNode(this);
        reader.accept(node, 0);
        return node;
    }

    public ClassFinder getClassFinder() {
        return this.classFinder;
    }

    private final ClassFinder classFinder;

    private final ConcurrentHashMap<String, TypeDefinition> objectTypes = new ConcurrentHashMap<>();

}
