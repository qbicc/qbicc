package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QChar;
import cc.quarkus.qcc.type.QInt16;
import cc.quarkus.qcc.type.QInt32;
import cc.quarkus.qcc.type.QInt64;
import cc.quarkus.qcc.type.QInt8;
import cc.quarkus.qcc.type.QIntegral;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class NarrowNode<INPUT_V extends QIntegral, OUTPUT_V extends QIntegral> extends AbstractNode<OUTPUT_V> {

    public static NarrowNode<QInt32, QInt8> i2b(Graph<?> graph, ControlNode<?> control, Node<QInt32> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.INT8);
    }

    public static NarrowNode<QInt32, QChar> i2c(Graph<?> graph, ControlNode<?> control, Node<QInt32> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.CHAR);
    }

    public static NarrowNode<QInt32, QInt16> i2s(Graph<?> graph, ControlNode<?> control, Node<QInt32> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.INT16);
    }

    public static NarrowNode<QInt64,QInt32> l2i(Graph<?> graph, ControlNode<?> control, Node<QInt64> node) {
        return new NarrowNode<>(graph, control, node, TypeDescriptor.INT32);
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
        if ( getTypeDescriptor() == TypeDescriptor.INT32) {
            return (OUTPUT_V) src.asInt32();
        }
        if ( getTypeDescriptor() == TypeDescriptor.INT16) {
            return (OUTPUT_V) src.asInt16();
        }
        if ( getTypeDescriptor() == TypeDescriptor.INT8) {
            return (OUTPUT_V) src.asInt8();
        }
        if ( getTypeDescriptor() == TypeDescriptor.CHAR ) {
            return (OUTPUT_V) src.asChar();
        }
        throw new RuntimeException( "Unable to narrow");
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of( getControl(), this.input);
    }

    @Override
    public String label() {
        return "<narrow:" + getId() + "> " + getTypeDescriptor();
    }

    private final Node<INPUT_V> input;
}
