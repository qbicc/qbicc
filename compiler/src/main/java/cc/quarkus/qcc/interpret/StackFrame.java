package cc.quarkus.qcc.interpret;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.QType;

public class StackFrame implements Context {

    protected StackFrame(InterpreterThread thread) {
        this.thread = thread;
    }

    @Override
    public <T extends QType> void set(Node<T> node, T value) {
        if( value instanceof SimpleInterpreterHeap) {
            System.err.println( this + " set " + node + " = " + value);
            new Exception().printStackTrace();

        }
        this.bindings.put(node, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends QType> T get(Node<T> node) {
        return (T) this.bindings.get(node);
    }

    @Override
    public InterpreterThread thread() {
        return this.thread;
    }

    public boolean contains(Node<?> node) {
        return this.bindings.containsKey(node);
    }

    private final InterpreterThread thread;

    private Map<Node<?>, Object> bindings = new HashMap<>();
}
