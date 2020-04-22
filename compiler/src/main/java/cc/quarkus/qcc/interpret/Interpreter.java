package cc.quarkus.qcc.interpret;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartValue;

public class Interpreter {

    public Interpreter(Graph graph) {
        this.graph = graph;
    }

    public EndToken execute(Object...arguments) {
        return new Thread().execute(this.graph, new StartValue(arguments));
    }

    private final Graph graph;
}
