package cc.quarkus.qcc.interpret;

import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.graph.type.StartToken;

public class Interpreter<V> {

    public Interpreter(Heap heap, Graph<V> graph) {
        this.heap = heap;
        this.graph = graph;
    }

    public EndToken<V> execute(Object...arguments) {
        return new Thread(this.heap).execute(this.graph, new StartToken(arguments));
    }

    public EndToken<V> execute(List<Object> arguments) {
        return execute(arguments.toArray());
    }

    private final Heap heap;
    private final Graph<V> graph;
}
