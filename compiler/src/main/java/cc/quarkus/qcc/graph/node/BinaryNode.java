package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public abstract class BinaryNode<INPUT_V extends QType, OUTPUT_V extends QType> extends AbstractNode<OUTPUT_V> {

    protected BinaryNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<OUTPUT_V> outType) {
        super(graph, control, outType);
    }

    public void setLHS(Node<INPUT_V> lhs) {
        this.lhs = lhs;
        lhs.addSuccessor(this);
    }

    public Node<INPUT_V> getLHS() {
        return this.lhs;
    }

    public void setRHS(Node<INPUT_V> rhs) {
        this.rhs = rhs;
        rhs.addSuccessor(this);
    }

    public Node<INPUT_V> getRHS() {
        return this.rhs;
    }

    public INPUT_V getLHSValue(Context context) {
        return this.lhs.getValue(context);
    }

    public INPUT_V getRHSValue(Context context) {
        return this.rhs.getValue(context);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl(), getLHS(), getRHS());
    }

    private Node<INPUT_V> lhs;

    private Node<INPUT_V> rhs;

}
