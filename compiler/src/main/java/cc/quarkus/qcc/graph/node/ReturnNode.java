package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.interpret.Context;

public class ReturnNode<V> extends AbstractNode<V> {
    public ReturnNode(ControlNode<?> control, Node<V> input) {
        super(control, input.getType());
        this.input = input;
        input.addSuccessor(this);
    }

    public Node<V> getInput() {
        return this.input;
    }

    @Override
    public V getValue(Context context) {
        return context.get(getInput());
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getInput() );
        }};
    }

    @Override
    public String label() {
        return getId() + ": <return> " + getType();
    }

    private final Node<V> input;
}
