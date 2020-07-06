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
import cc.quarkus.qcc.type.universe.Universe;
import org.junit.Test;

public class UniverseTest {

    static int add(int l, int r) {
        return l + r;
    }

    @Test
    public void testMethod() throws Exception {
        Context context = new Context(false);
        context.run(() -> {
            ClassLoaderClassFinder classFinder = new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader());
            Universe universe = new Universe(classFinder);
            Universe.setRootUniverse(universe);
            // todo: remove once we have proper JVM init
            initialize(universe);
            //
            defineInitialClass(universe, "cc/quarkus/qcc/type/UniverseTest");
            defineInitialClass(universe, "cc/quarkus/qcc/type/universe/Universe");
            // end todo
            DefinedTypeDefinition cls = universe.findClass("cc/quarkus/qcc/type/UniverseTest");
            ResolvedMethodDefinition method = cls.verify().resolve().resolveMethod(MethodIdentifier.of("add", MethodTypeDescriptor.of(Type.S32, Type.S32, Type.S32)));
            assertThat(method.hasMethodBody());
        });
    }

    static void initialize(final Universe universe) throws IOException, ClassNotFoundException {
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

    private static void defineInitialClass(Universe universe, String className) throws IOException, ClassNotFoundException {
        universe.defineClass(className, ByteBuffer.wrap(universe.getClassFinder().findClass(className).readAllBytes()));
    }

    @Test
    public void testResolveMethod() throws Exception {
        Context context = new Context(false);
        context.run(() -> {
            Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
            Universe.setRootUniverse(universe);
            initialize(universe);
            //
            defineInitialClass(universe, "cc/quarkus/qcc/type/MyClass");
            defineInitialClass(universe, "cc/quarkus/qcc/type/MyOtherClass");
            DefinedTypeDefinition obj = universe.findClass("java/lang/Object");
            DefinedTypeDefinition myClass = universe.findClass("cc/quarkus/qcc/type/MyClass");
            MethodIdentifier identifier = MethodIdentifier.of("equals", MethodTypeDescriptor.of(Type.BOOL, obj.verify().getClassType()));
            ResolvedMethodDefinition method = myClass.verify().resolve().resolveMethod(identifier);
            assertThat(method.getEnclosingTypeDefinition().getName()).isEqualTo(myClass.getName());
            DefinedTypeDefinition myOtherClass = universe.findClass("cc/quarkus/qcc/type/MyOtherClass");
            method = myOtherClass.verify().resolve().resolveMethod(identifier);
            assertThat(method.getEnclosingTypeDefinition().getName()).isEqualTo(obj.getName());
        });
    }
}
