package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class WidenNode<INPUT_V, OUTPUT_V> extends AbstractNode<OUTPUT_V> {

    public WidenNode(ControlNode<?> control, Node<INPUT_V> input, TypeDescriptor<OUTPUT_V> outType) {
        super(control, outType);
        this.input = input;
        input.addSuccessor(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public OUTPUT_V getValue(Context context) {
        INPUT_V src = context.get(this.input);
        if ( getTypeDescriptor() == TypeDescriptor.LONG ) {
            return (OUTPUT_V) Long.valueOf(((Number)src).longValue());
        }
        if ( getTypeDescriptor() == TypeDescriptor.INT ) {
            return (OUTPUT_V) Integer.valueOf(((Number)src).intValue());
        }
        if ( getTypeDescriptor() == TypeDescriptor.SHORT ) {
            return (OUTPUT_V) Short.valueOf(((Number)src).shortValue());
        }
        throw new RuntimeException( "Unable to widen");
        //return (OUTPUT_V) src;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
            add(input);
        }};
    }

    @Override
    public String label() {
        return "<widen:" + getId() + "> " + getTypeDescriptor();
    }

    private final Node<INPUT_V> input;
}
