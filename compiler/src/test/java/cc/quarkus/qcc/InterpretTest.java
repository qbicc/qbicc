package cc.quarkus.qcc;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.DotWriter;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.interpret.Interpreter;
import cc.quarkus.qcc.parse.BytecodeParser;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.Universe;
import org.junit.Test;

public class InterpretTest {

    @Test
    public void testStaticMethod() throws FileNotFoundException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyClass");
        MethodDefinition m = c.getMethod("min", "(II)I");

        BytecodeParser parser = new BytecodeParser(m);
        Graph graph = parser.parse();

        try (DotWriter writer = new DotWriter(Paths.get("target/graph.dot"))) {
            writer.write(graph);
        }

        Interpreter interp = new Interpreter();
        EndToken result = interp.execute(graph, new StartValue(42, 88));
        System.err.println( "result ==> " + result);
        //graph.interpret(graph.getStart().getType().newInstance())
    }
}
