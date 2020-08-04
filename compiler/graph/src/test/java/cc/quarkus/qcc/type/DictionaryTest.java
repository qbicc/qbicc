package cc.quarkus.qcc.type;

import static org.fest.assertions.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolvedMethodDefinition;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
import cc.quarkus.qcc.type.definition.Dictionary;
import org.junit.Test;

public class DictionaryTest {

    static int add(int l, int r) {
        return l + r;
    }

    // this holder prevents the test from failing
    static class Holder {
        static final ClassLoaderClassFinder classFinder = new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void testMethod() throws Exception {
        Context context = new Context(false);
        context.run(() -> {
            Dictionary dictionary = new Dictionary();
            Dictionary.setRootDictionary(dictionary);
            // todo: remove once we have proper JVM init
            initialize(dictionary);
            //
            defineInitialClass(dictionary, "cc/quarkus/qcc/type/DictionaryTest");
            defineInitialClass(dictionary, "cc/quarkus/qcc/type/definition/Dictionary");
            // end todo
            DefinedTypeDefinition cls = dictionary.findClass("cc/quarkus/qcc/type/DictionaryTest");
            ResolvedMethodDefinition method = cls.verify().resolve().resolveMethod(MethodIdentifier.of("add", MethodTypeDescriptor.of(Type.S32, Type.S32, Type.S32)));
            assertThat(method.hasMethodBody());
        });
    }

    static void initialize(final Dictionary dictionary) throws IOException {
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

    private static void defineInitialClass(Dictionary dictionary, String className) throws IOException {
        dictionary.defineClass(className, ByteBuffer.wrap(Holder.classFinder.findClass(className).readAllBytes()));
    }

    @Test
    public void testResolveMethod() throws Exception {
        Context context = new Context(false);
        context.run(() -> {
            Dictionary dictionary = new Dictionary();
            Dictionary.setRootDictionary(dictionary);
            initialize(dictionary);
            //
            defineInitialClass(dictionary, "cc/quarkus/qcc/type/MyClass");
            defineInitialClass(dictionary, "cc/quarkus/qcc/type/MyOtherClass");
            DefinedTypeDefinition obj = dictionary.findClass("java/lang/Object");
            DefinedTypeDefinition myClass = dictionary.findClass("cc/quarkus/qcc/type/MyClass");
            MethodIdentifier identifier = MethodIdentifier.of("equals", MethodTypeDescriptor.of(Type.BOOL, obj.verify().getClassType()));
            ResolvedMethodDefinition method = myClass.verify().resolve().resolveMethod(identifier);
            assertThat(method.getEnclosingTypeDefinition().getName()).isEqualTo(myClass.getName());
            DefinedTypeDefinition myOtherClass = dictionary.findClass("cc/quarkus/qcc/type/MyOtherClass");
            method = myOtherClass.verify().resolve().resolveMethod(identifier);
            assertThat(method.getEnclosingTypeDefinition().getName()).isEqualTo(obj.getName());
        });
    }
}
