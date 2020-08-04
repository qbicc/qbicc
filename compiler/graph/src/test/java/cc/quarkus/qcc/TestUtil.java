package cc.quarkus.qcc;

import cc.quarkus.qcc.type.universe.Universe;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TestUtil {
    public static void initialize(final Universe universe) throws IOException {
        defineInitialClass(universe, "java/lang/Object");
        defineInitialClass(universe, "java/lang/Class");
        defineInitialClass(universe, "java/io/Serializable");
        defineInitialClass(universe, "java/lang/reflect/GenericDeclaration");
        defineInitialClass(universe, "java/lang/reflect/AnnotatedElement");
        defineInitialClass(universe, "java/lang/reflect/Type");
        defineInitialClass(universe, "java/lang/String");
        defineInitialClass(universe, "java/lang/Comparable");
        defineInitialClass(universe, "java/lang/CharSequence");
    }

    public static void defineInitialClass(Universe universe, String className) throws IOException {
        // temporary hackery...
        try (InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(className + ".class")) {
            universe.defineClass(className, ByteBuffer.wrap(is.readAllBytes()));
        }
    }
}
