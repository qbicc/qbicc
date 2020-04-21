package cc.quarkus.qcc.interpret;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartValue;

public class Interpreter {

    public Interpreter() {

    }

    public EndToken execute(Graph graph, StartValue arguments) {
        Thread thread = new Thread();
        return thread.execute(graph, arguments);
    }

    private List<Thread> threads = new ArrayList<>();
}
