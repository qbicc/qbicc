package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.QIntegral;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class WidenNode<INPUT_V extends QIntegral, OUTPUT_V extends QIntegral> extends AbstractNode<OUTPUT_V> {

    public static WidenNode<QInt32, QInt64> i2l(Graph<?> graph, ControlNode<?> control, Node<QInt32> node) {
        return new WidenNode<>(graph, control, node, TypeDescriptor.INT64);
    }

    public static WidenNode<QInt8,QInt32> b2i(Graph<?> graph, ControlNode<?> control, Node<QInt8> node) {
        return new WidenNode<>(graph, control, node, TypeDescriptor.INT32);
    }

    public WidenNode(Graph<?> graph, ControlNode<?> control, Node<INPUT_V> input, TypeDescriptor<OUTPUT_V> outType) {
        super(graph, control, outType);
        this.input = input;
        input.addSuccessor(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public OUTPUT_V getValue(Context context) {
        INPUT_V src = context.get(this.input);
        if ( getTypeDescriptor() == TypeDescriptor.INT64) {
            return (OUTPUT_V) src.asInt64();
        }
        if ( getTypeDescriptor() == TypeDescriptor.INT32) {
            return (OUTPUT_V) src.asInt32();
        }
        if ( getTypeDescriptor() == TypeDescriptor.INT16) {
            return (OUTPUT_V) src.asInt16();
        }
        throw new RuntimeException( "Unable to widen");
        //return (OUTPUT_V) src;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl(), this.input);
    }

    @Override
    public String label() {
        return "<widen:" + getId() + "> " + getTypeDescriptor();
    }

    private final Node<INPUT_V> input;
}
