package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.interpret.Context;

public abstract class BinaryNode<INPUT_V, OUTPUT_V> extends AbstractNode<OUTPUT_V> {

    protected BinaryNode(ControlNode<?> control, Class<OUTPUT_V> outType) {
        super(control, outType);
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

    public INPUT_V getLHSValue(Context context ) {
        return this.lhs.getValue(context);
    }

    public INPUT_V getRHSValue(Context context) {
        return this.rhs.getValue(context);
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getLHS() );
            add( getRHS() );
        }};
    }

    private Node<INPUT_V> lhs;
    private Node<INPUT_V> rhs;

}
