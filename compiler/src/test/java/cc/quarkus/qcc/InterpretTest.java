package cc.quarkus.qcc;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.IntValue;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.parse.BytecodeParser;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.Universe;
import org.junit.Test;

public class InterpretTest {

    @Test
    public void testStaticMethod() {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyClass");
        MethodDefinition m = c.getMethod("min", "(II)I");

        BytecodeParser parser = new BytecodeParser(m);
        Graph graph = parser.parse();

        StartValue context = graph.getStart().getType().newInstance(new IntValue(42), new IntValue(88));
        graph.execute(context);
        //graph.interpret(graph.getStart().getType().newInstance())
    }
}
