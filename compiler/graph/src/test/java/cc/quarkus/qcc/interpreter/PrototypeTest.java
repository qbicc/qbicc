package cc.quarkus.qcc.interpreter;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;
import org.fest.assertions.core.Condition;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;

import static cc.quarkus.qcc.TestUtil.defineInitialClass;
import static cc.quarkus.qcc.TestUtil.initialize;
import static cc.quarkus.qcc.interpreter.CodegenUtils.p;
import static org.fest.assertions.api.Assertions.assertThat;

public class PrototypeTest {
    Universe universe;

    @Before
    public void before() throws Exception {
        ClassLoaderClassFinder classFinder = new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader());
        Universe universe = new Universe(classFinder);
        Universe.setRootUniverse(universe);

        initialize(universe);
    }

    @Test
    public void testPrototype() throws Exception {
        Context context = new Context(false);

        context.run(() -> {
            String classWithFields = p(ClassWithFields.class);

            defineInitialClass(universe, classWithFields);

            DefinedTypeDefinition myClass = universe.findClass(classWithFields);

            Prototype proto = PrototypeGenerator.getPrototype(myClass);

            byte[] bytecode = proto.getBytecode();

            Class protoClass = new TestClassLoader().defineClass(classWithFields.replace('/', '.'), bytecode);

            Arrays.stream(ClassWithFields.class.getDeclaredFields()).forEach((field) -> {
                Field protoField = getDeclaredField(protoClass, field.getName());

                assertThat(protoField).isNotNull();
                assertThat(protoField.getType()).is(equivalent(field.getType()));
                assertThat(protoField.getModifiers()).isEqualTo(field.getModifiers());
            });
        });
    }

    private static Condition<Class<?>> equivalent(Class<?> expected) {
        return new Condition<>() {
            @Override
            public boolean matches(Class<?> actual) {
                if (expected.isPrimitive()) {
                    if (expected == boolean.class) {
                        return actual == boolean.class;
                    }
                    if (expected == char.class) {
                        return actual == short.class;
                    }
                    return actual == expected;
                } else {
                    return actual == Object.class;
                }
            }
        };
    }

    private static <T extends Throwable> Field getDeclaredField(Class cls, String name) throws T {
        try {
            return cls.getDeclaredField(name);
        } catch (Throwable t) {
            throwsUnchecked(t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void throwsUnchecked(final Throwable e) throws T {
        throw (T) e;
    }

    private static class TestClassLoader extends ClassLoader {
        public Class defineClass(String name, byte[] bytecode) {
            Class cls = super.defineClass(name, bytecode, 0, bytecode.length);
            super.resolveClass(cls);
            return cls;
        }
    }
}
