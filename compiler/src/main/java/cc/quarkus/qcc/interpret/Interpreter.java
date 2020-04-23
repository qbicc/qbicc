package cc.quarkus.qcc.interpret;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartToken;

public class Interpreter {

    public Interpreter(Graph graph) {
        this.graph = graph;
    }

    public EndToken execute(Object...arguments) {
        return new Thread().execute(this.graph, new StartToken(arguments));
    }

    private final Graph graph;
}
