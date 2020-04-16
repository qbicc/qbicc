package cc.quarkus.qcc.interpret;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.EndValue;
import cc.quarkus.qcc.graph.type.StartValue;

public class Interpreter {

    public Interpreter() {

    }

    public EndValue execute(Graph graph, StartValue arguments) {
        Thread thread = new Thread();
        return thread.execute(graph, arguments);
    }

    private List<Thread> threads = new ArrayList<>();
}
