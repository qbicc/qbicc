package cc.quarkus.qcc.type;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.type.definition.MethodDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.MethodDescriptorParser;
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
        MethodDefinition method = cls.findMethod("add", "(II)I");
        assertThat(method.getGraph()).isNotNull();
    }

    @Test
    public void testResolveMethod() {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition obj = universe.findClass("java/lang/Object");
        TypeDefinition myClass = universe.findClass("cc/quarkus/qcc/type/MyClass");
        MethodDescriptorParser parser = new MethodDescriptorParser(universe, obj, "equals", "(Ljava/lang/Object;)Z", false);
        MethodDescriptor desc = parser.parseMethodDescriptor();

        MethodDefinition method = myClass.resolveMethod(desc);
        assertThat(method.getOwner().getName()).isEqualTo(myClass.getName());

        TypeDefinition myOtherClass = universe.findClass("cc/quarkus/qcc/type/MyOtherClass");

        method = myOtherClass.resolveMethod(desc);
        assertThat(method.getOwner().getName()).isEqualTo(obj.getName());
    }
}
