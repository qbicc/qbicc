package cc.quarkus.qcc;

import java.io.IOException;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.type.CallResult;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.Universe;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class InterpretTest {

    @Test
    public void testStaticMethod() throws IOException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyClass");
        MethodDefinition m = c.findMethod("min", "(II)I");

        m.writeGraph("target/");

        CallResult result = m.call(42, 88);
        assertThat(result.getReturnValue()).isEqualTo(42);
    }

    @Test
    public void testThrows() throws IOException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyThrowingClass");
        //MethodDefinition m = c.getMethod("foo", "()I");
        MethodDefinition m = c.findMethod("foo", "()I");
        m.writeGraph("target/");
        //CallResult result = m.call();
        //System.err.println(result);
    }
}
