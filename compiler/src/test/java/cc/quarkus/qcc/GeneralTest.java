package cc.quarkus.qcc;

import java.io.FileNotFoundException;
import java.nio.file.Paths;

import cc.quarkus.qcc.finders.ClassLoaderClassFinder;
import cc.quarkus.qcc.graph.DotWriter;
import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.parse.BytecodeParser;
import cc.quarkus.qcc.type.MethodDefinition;
import cc.quarkus.qcc.type.TypeDefinition;
import cc.quarkus.qcc.type.Universe;
import org.junit.Test;

public class GeneralTest {

    @Test
    public void testIt() throws FileNotFoundException {
        Universe universe = new Universe(new ClassLoaderClassFinder(Thread.currentThread().getContextClassLoader()));
        TypeDefinition c = universe.findClass("cc/quarkus/qcc/MyClass");
        MethodDefinition m = c.getMethod("sum", "(II)I");

        System.err.println("method: " + m);

        BytecodeParser parser = new BytecodeParser(m);
        Graph graph = parser.parse();

        try (DotWriter writer = new DotWriter(Paths.get("target/graph.dot"))) {
            writer.write(graph);
        }
    }
}
