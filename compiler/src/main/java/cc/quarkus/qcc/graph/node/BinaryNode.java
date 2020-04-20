package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public abstract class BinaryNode<
        INPUT_T extends Type<INPUT_T>, INPUT_V extends Value<INPUT_T, INPUT_V>,
        T extends Type<T>, V extends Value<T,V>> extends AbstractNode<T,V> {

    protected BinaryNode(ControlNode<?,?> control, T outType) {
        super(control, outType);
    }

    public void setLHS(Node<INPUT_T,INPUT_V> lhs) {
        this.lhs = lhs;
        lhs.addSuccessor(this);
    }

    public Node<INPUT_T, INPUT_V> getLHS() {
        return this.lhs;
    }

    public void setRHS(Node<INPUT_T,INPUT_V> rhs) {
        this.rhs = rhs;
        rhs.addSuccessor(this);
    }

    public Node<INPUT_T, INPUT_V> getRHS() {
        return this.rhs;
    }

    public INPUT_V getLHSValue(Context context ) {
        return this.lhs.getValue(context);
    }

    public INPUT_V getRHSValue(Context context) {
        return this.rhs.getValue(context);
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return new ArrayList<>() {{
            add( getControl() );
            add( getLHS() );
            add( getRHS() );
        }};
    }

    private Node<INPUT_T, INPUT_V> lhs;
    private Node<INPUT_T, INPUT_V> rhs;

}
