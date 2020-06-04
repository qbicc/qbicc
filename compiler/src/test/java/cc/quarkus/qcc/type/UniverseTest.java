package cc.quarkus.qcc.type;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.universe.Universe;
import org.junit.Test;

import static org.fest.assertions.api.Assertions.*;

public class UniverseTest {

    static int add(int l, int r) {
        return l + r;
    }

    @Test
    public void testMethod() {

        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition cls = universe.findClass("cc/quarkus/qcc/type/UniverseTest");
        MethodDefinition<?> method = cls.findMethod("add", "(II)I");

        assertThat(method.getGraph()).isNotNull();
    }
}
