package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class ReturnNode<T extends ConcreteType<T>, V extends Value<T,V>> extends AbstractNode<T,V> {
    public ReturnNode(ControlNode<?,?> control, T outType, Node<T,V> input) {
        super(control, outType);
        this.input = input;
        input.addSuccessor(this);
    }

    public Node<T, V> getInput() {
        return this.input;
    }

    @Override
    public V getValue(Context context) {
        return context.get(getInput());
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getInput() );
        }};
    }

    @Override
    public String label() {
        return getId() + ": <return> " + getType().label();
    }

    private final Node<T, V> input;
}
