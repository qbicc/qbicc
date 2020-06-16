package cc.quarkus.qcc.type.universe;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.spi.ClassFinder;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.LinkageException;
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

    public DefinedTypeDefinition findClass(String name) throws LinkageException {
        // fast path
        DefinedTypeDefinition loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        // todo: call into VM to load class
        throw new IllegalStateException("No VM is present");
    }

    public DefinedTypeDefinition findLoadedClass(String name) {
        return objectTypes.get(name);
    }

    public DefinedTypeDefinition defineClass(String name, ByteBuffer buffer) throws LinkageException {
        DefinedTypeDefinition def = DefinedTypeDefinition.create(this, name, buffer);
        if (objectTypes.putIfAbsent(name, def) != null) {
            throw new DefineFailedException("Duplicated class named " + name);
        }
        return def;
    }

    public ClassFinder getClassFinder() {
        return this.classFinder;
    }

    private final ClassFinder classFinder;

    private final ConcurrentHashMap<String, DefinedTypeDefinition> objectTypes = new ConcurrentHashMap<>();

}
