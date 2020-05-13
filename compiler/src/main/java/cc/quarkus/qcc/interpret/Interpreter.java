package cc.quarkus.qcc.interpret;

import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.EndToken;
import cc.quarkus.qcc.type.QType;

public class Interpreter<V extends QType> {

    public Interpreter(InterpreterHeap heap, Graph<V> graph) {
        this.heap = heap;
        this.graph = graph;
    }

    public EndToken<V> execute(Object...arguments) {
        return new InterpreterThread(this.heap).execute(this.graph, arguments);
    }

    public EndToken<V> execute(List<Object> arguments) {
        return new InterpreterThread(this.heap).execute(this.graph, arguments);
    }

    private final InterpreterHeap heap;
    private final Graph<V> graph;
}
