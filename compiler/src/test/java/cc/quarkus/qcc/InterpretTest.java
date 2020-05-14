package cc.quarkus.qcc;

import java.io.IOException;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.interpret.CallResult;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class InterpretTest {

    @Test
    public void testStaticMethod() throws IOException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyClass");
        MethodDefinition<QInt32> m = (MethodDefinition<QInt32>) c.findMethod("min", "(II)I");

        m.writeGraph("target/");

        CallResult<QInt32> result = m.call(QInt32.of(42), QInt32.of(88));
        assertThat(result.getReturnValue().value()).isEqualTo(42);
    }

    @Test
    public void testThrows() throws IOException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyThrowingClass");
        //MethodDefinition m = c.getMethod("foo", "()I");
        MethodDefinition<?> m = c.findMethod("foo", "()I");
        m.writeGraph("target/");
        CallResult<?> result = m.call();
        assertThat(result.getReturnValue()).isNull();
        assertThat(result.getThrowValue()).isNotNull();
        //System.err.println(result);
    }
}
