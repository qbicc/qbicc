package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class WidenNode<T extends Type<T>, V extends Value<T,V> > extends AbstractNode<T, V> {

    public WidenNode(ControlNode<?, ?> control, Node<?, ?> input, T outType) {
        super(outType);
        this.control = control;
        this.input = input;
        control.addSuccessor(this);
        input.addSuccessor(this);
    }

    @Override
    public V getValue(Context context) {
        Value<?, ?> src = context.get(this.input);
        return (V) getType().coerce(src);
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return new ArrayList<>() {{
            add(control);
            add(input);
        }};
    }

    private final ControlNode<?, ?> control;

    private final Node<?, ?> input;
}
