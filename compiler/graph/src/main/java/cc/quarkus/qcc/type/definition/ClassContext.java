package cc.quarkus.qcc.type.definition;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;

/**
 * A class and interface context, which can either be standalone (static) or can be integrated with an interpreter.  An
 * interpreter should have one instance per class loader.
 */
public interface ClassContext {
    DefinedTypeDefinition findDefinedType(String typeName);

    DefinedTypeDefinition resolveDefinedTypeLiteral(TypeIdLiteral typeId);

    String deduplicate(ByteBuffer buffer, int offset, int length);

    String deduplicate(String original);

    TypeSystem getTypeSystem();

    LiteralFactory getLiteralFactory();

    /**
     * Create a basic class context which can be used to produce type definitions.
     *
     * @param jarPaths the JAR paths to search
     * @return the class context
     * @throws IOException if one of the JAR paths could not be opened
     */
    static Basic createBasic(List<Path> jarPaths) throws IOException {
        List<JarFile> jarFiles = new ArrayList<>(jarPaths.size());
        int i = 0;
        try {
            while (i < jarPaths.size()) {
                Path path = jarPaths.get(i);
                JarFile jarFile = new JarFile(path.toFile(), false, ZipFile.OPEN_READ, JarFile.runtimeVersion());
                jarFiles.set(i++, jarFile);
            }
        } catch (Throwable t) {
            while (i > 0) {
                try {
                    jarFiles.get(--i).close();
                } catch (IOException e) {
                    t.addSuppressed(e);
                }
            }
            throw t;
        }

        return new Basic() {
            final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
            final TypeSystem ts = TypeSystem.builder().build();
            final LiteralFactory lf = LiteralFactory.create(ts);

            public void close() {
                for (JarFile jarFile : jarFiles) {
                    try {
                        jarFile.close();
                    } catch (Throwable ignored) {
                    }
                }
            }

            public DefinedTypeDefinition findDefinedType(final String typeName) {
                for (JarFile jarFile : jarFiles) {
                    JarEntry entry = jarFile.getJarEntry(typeName + ".class");
                    if (entry != null) try {
                        byte[] content = jarFile.getInputStream(entry).readAllBytes();
                        DefinedTypeDefinition.Builder builder = DefinedTypeDefinition.Builder.basic();
                        ClassFile.of(this, ByteBuffer.wrap(content)).accept(builder);
                        return builder.build();
                    } catch (IOException e) {
                        throw new DefineFailedException(e);
                    }
                }
                return null;
            }

            public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
                if (typeId instanceof ClassTypeIdLiteral) {
                    return findDefinedType(((ClassTypeIdLiteral) typeId).getInternalName());
                } else if (typeId instanceof InterfaceTypeIdLiteral) {
                    return findDefinedType(((InterfaceTypeIdLiteral) typeId).getInternalName());
                } else {
                    throw new IllegalArgumentException("Invalid type ID literal");
                }
            }

            public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
                byte[] b = new byte[length];
                int p = buffer.position();
                try {
                    buffer.position(offset);
                    buffer.get(b);
                } finally {
                    buffer.position(p);
                }
                return deduplicate(new String(b, StandardCharsets.UTF_8));
            }

            public String deduplicate(final String original) {
                return cache.computeIfAbsent(original, Function.identity());
            }

            public TypeSystem getTypeSystem() {
                return ts;
            }

            public LiteralFactory getLiteralFactory() {
                return lf;
            }
        };
    }

    interface Basic extends ClassContext, AutoCloseable {
        void close();
    }
}
