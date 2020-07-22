package cc.quarkus.qcc.type;

import static cc.quarkus.qcc.TestUtil.defineInitialClass;
import static cc.quarkus.qcc.TestUtil.initialize;
import static org.fest.assertions.api.Assertions.*;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;

import cc.quarkus.qcc.TestUtil;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.Prototype;
import cc.quarkus.qcc.interpreter.PrototypeGenerator;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.ResolvedMethodDefinition;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import org.fest.assertions.error.ShouldNotBeNull;
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
