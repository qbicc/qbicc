package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.LinkageException;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import io.smallrye.common.constraint.Assert;
import org.objectweb.asm.Opcodes;

public class Dictionary {

    public static final int ASM_VERSION = Opcodes.ASM7;

    private final JavaObject classLoader;

    Dictionary() {
        classLoader = null;
    }

    public Dictionary(JavaObject classLoader) {
        this.classLoader = Assert.checkNotNullParam("classLoader", classLoader);
    }

    public DefinedTypeDefinition findClass(String name) throws LinkageException {
        // fast path
        DefinedTypeDefinition loaded = findLoadedClass(name);
        if (loaded != null) {
            return loaded;
        }
        // todo: call into VM to load class
        throw new IllegalStateException("No VM is present to load " + name);
    }

    public DefinedTypeDefinition findLoadedClass(String name) {
        return objectTypes.get(name);
    }

    public DefinedTypeDefinition tryDefineClass(final String name, final ByteBuffer buffer) {
        if (objectTypes.containsKey(name)) {
            return null;
        }
        JavaVM vm = JavaVM.requireCurrent();

        ClassFile classFile = ClassFile.of(buffer, classLoader);
        DefinedTypeDefinition.Builder builder = vm.newTypeDefinitionBuilder(classLoader);
        classFile.accept(builder);
        DefinedTypeDefinition def = builder.build();
        if (objectTypes.putIfAbsent(name, def) != null) {
            return null;
        }
        return def;
    }

    public DefinedTypeDefinition defineClass(String name, ByteBuffer buffer) throws LinkageException {
        if (objectTypes.containsKey(name)) {
            throw new DefineFailedException("Duplicated class named " + name);
        }
        JavaVM vm = JavaVM.requireCurrent();

        ClassFile classFile = ClassFile.of(buffer, classLoader);
        DefinedTypeDefinition.Builder builder = vm.newTypeDefinitionBuilder(classLoader);
        classFile.accept(builder);
        DefinedTypeDefinition def = builder.build();
        if (objectTypes.putIfAbsent(name, def) != null) {
            throw new DefineFailedException("Duplicated class named " + name);
        }
        return def;
    }

    public boolean replaceTypeDefinition(final String name, final DefinedTypeDefinition oldVal, final DefinedTypeDefinition newVal) {
        return objectTypes.replace(name, oldVal, newVal);
    }

    public Type parseSingleDescriptor(final String descriptor) {
        return parseSingleDescriptor(descriptor, 0);
    }

    Type parseSingleDescriptor(final String descriptor, int idx) {
        if (idx >= descriptor.length()) {
            throw new RuntimeException("Empty type descriptor");
        }
        switch ( descriptor.charAt(idx) ) {
            case 'Z':
                return Type.BOOL;
            case 'B':
                return Type.S8;
            case 'C':
                return Type.U16;
            case 'S':
                return Type.S16;
            case 'I':
                return Type.S32;
            case 'J':
                return Type.S64;
            case 'F':
                return Type.F32;
            case 'D':
                return Type.F64;
            case 'V':
                return Type.VOID;
            case 'L':
                int term = descriptor.indexOf(';');
                if (term == -1) {
                    throw new RuntimeException("Unterminated type descriptor");
                }
                if (term < descriptor.length() - 1) {
                    throw new RuntimeException("Extra type information");
                }
                String className = descriptor.substring(idx + 1, term);
                return findClass(className).verify().getClassType();
            case '[':
                return Type.arrayOf(parseSingleDescriptor(descriptor, idx + 1));
            default:
                throw new RuntimeException("Unable to parse: " + descriptor + " at " + idx );
        }
    }

    private final ConcurrentHashMap<String, DefinedTypeDefinition> objectTypes = new ConcurrentHashMap<>();
}
