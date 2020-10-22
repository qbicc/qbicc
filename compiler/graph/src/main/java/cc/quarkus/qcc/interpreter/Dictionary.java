package cc.quarkus.qcc.interpreter;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefineFailedException;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.LinkageException;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import io.smallrye.common.constraint.Assert;

public class Dictionary implements ClassContext {

    private final ConcurrentHashMap<String, DefinedTypeDefinition> typesByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TypeIdLiteral, DefinedTypeDefinition> typesByLiteral = new ConcurrentHashMap<>();
    private final JavaObject classLoader;
    private final JavaVMImpl vm;

    Dictionary(final JavaVMImpl vm) {
        this.vm = vm;
        classLoader = null;
    }

    Dictionary(JavaObject classLoader, final JavaVMImpl vm) {
        this.classLoader = Assert.checkNotNullParam("classLoader", classLoader);
        this.vm = vm;
    }

    public DefinedTypeDefinition findDefinedType(String name) throws LinkageException {
        // fast path
        DefinedTypeDefinition loaded = findLoadedType(name);
        if (loaded == null) {
            loaded = vm.loadClass(classLoader, name);
            typesByName.put(name, loaded);
            typesByLiteral.put(loaded.validate().getTypeId(), loaded);
        }
        return loaded;
    }

    public DefinedTypeDefinition findLoadedType(String name) {
        return typesByName.get(name);
    }

    public DefinedTypeDefinition tryDefineClass(final String name, final ByteBuffer buffer) {
        if (typesByName.containsKey(name)) {
            return null;
        }
        JavaVM vm = JavaVM.requireCurrent();

        ClassFile classFile = ClassFile.of(this, buffer);
        DefinedTypeDefinition.Builder builder = vm.newTypeDefinitionBuilder(classLoader);
        classFile.accept(builder);
        DefinedTypeDefinition def = builder.build();
        if (typesByName.putIfAbsent(name, def) != null) {
            return null;
        }
        return def;
    }

    public DefinedTypeDefinition defineClass(String name, ByteBuffer buffer) throws LinkageException {
        if (typesByName.containsKey(name)) {
            throw new DefineFailedException("Duplicated class named " + name);
        }
        JavaVM vm = JavaVM.requireCurrent();

        ClassFile classFile = ClassFile.of(this, buffer);
        DefinedTypeDefinition.Builder builder = vm.newTypeDefinitionBuilder(classLoader);
        classFile.accept(builder);
        DefinedTypeDefinition def = builder.build();
        if (typesByName.putIfAbsent(name, def) != null) {
            throw new DefineFailedException("Duplicated class named " + name);
        }
        return def;
    }

    public boolean replaceTypeDefinition(final String name, final DefinedTypeDefinition oldVal, final DefinedTypeDefinition newVal) {
        return typesByName.replace(name, oldVal, newVal);
    }

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
        return typesByLiteral.get(typeId);
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        // todo

        return null;
    }

    public String deduplicate(final String original) {
        // todo
        return original;
    }

    public TypeSystem getTypeSystem() {
        return vm.getTypeSystem();
    }

    public void registerClassLiteral(final ClassTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {

    }

    public void registerInterfaceLiteral(final InterfaceTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {

    }

    public LiteralFactory getLiteralFactory() {
        return vm.getLiteralFactory();
    }

    public BasicBlockBuilder newBasicBlockBuilder() {
        return BasicBlockBuilder.simpleBuilder(getTypeSystem());
    }

    public void defineClass(final String name, final DefinedTypeDefinition definition) {

    }

    public JavaObject getClassLoader() {
        return classLoader;
    }
}
