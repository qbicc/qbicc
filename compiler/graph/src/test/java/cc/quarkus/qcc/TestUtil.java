package cc.quarkus.qcc;

import cc.quarkus.qcc.type.definition.Dictionary;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class TestUtil {
    public static void initialize(final Dictionary dictionary) throws IOException {
        defineInitialClass(dictionary, "java/lang/Object");
        defineInitialClass(dictionary, "java/lang/Class");
        defineInitialClass(dictionary, "java/io/Serializable");
        defineInitialClass(dictionary, "java/lang/reflect/GenericDeclaration");
        defineInitialClass(dictionary, "java/lang/reflect/AnnotatedElement");
        defineInitialClass(dictionary, "java/lang/reflect/Type");
        defineInitialClass(dictionary, "java/lang/String");
        defineInitialClass(dictionary, "java/lang/Comparable");
        defineInitialClass(dictionary, "java/lang/CharSequence");
    }

    public static void defineInitialClass(Dictionary dictionary, String className) throws IOException {
        // temporary hackery...
        try (InputStream is = TestUtil.class.getClassLoader().getResourceAsStream(className + ".class")) {
            dictionary.defineClass(className, ByteBuffer.wrap(is.readAllBytes()));
        }
    }
}
