package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class NarrowNode<INPUT_V, OUTPUT_V> extends AbstractNode<OUTPUT_V> {

    public static NarrowNode<Integer,Byte> i2b(Graph<?> graph, ControlNode<?> control, Node<Integer> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.BYTE);
    }

    public static NarrowNode<Integer,Character> i2c(Graph<?> graph, ControlNode<?> control, Node<Integer> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.CHAR);
    }

    public static NarrowNode<Integer,Short> i2s(Graph<?> graph, ControlNode<?> control, Node<Integer> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.SHORT);
    }

    public static NarrowNode<Long,Integer> l2i(Graph<?> graph, ControlNode<?> control, Node<Long> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.INT);
    }

    public NarrowNode(Graph<?> graph, ControlNode<?> control, Node<INPUT_V> input, TypeDescriptor<OUTPUT_V> outType) {
        super(graph, control, outType);
        this.input = input;
        input.addSuccessor(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public OUTPUT_V getValue(Context context) {
        INPUT_V src = context.get(this.input);
        if ( getTypeDescriptor() == TypeDescriptor.INT ) {
            return (OUTPUT_V) Integer.valueOf(((Number)src).intValue());
        }
        if ( getTypeDescriptor() == TypeDescriptor.SHORT ) {
            return (OUTPUT_V) Short.valueOf(((Number)src).shortValue());
        }
        if ( getTypeDescriptor() == TypeDescriptor.BYTE ) {
            return (OUTPUT_V) Byte.valueOf(((Number)src).byteValue());
        }
        if ( getTypeDescriptor() == TypeDescriptor.CHAR ) {
            return (OUTPUT_V) Character.valueOf((char) ((Number)src).shortValue());
        }
        throw new RuntimeException( "Unable to narrow");
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
        return "<narrow:" + getId() + "> " + getTypeDescriptor();
    }

    private final Node<INPUT_V> input;
}
