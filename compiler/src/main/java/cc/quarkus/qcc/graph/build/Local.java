package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;

public abstract class Local {

    public Local(ControlNode<?> control, int index) {
        this.control = control;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public abstract void store(Node<?> val);

    public abstract <V> Node<V> load(Class<V> type);

    public abstract <V> Node<V> get(Class<V> type);

    public abstract Local duplicate();

    protected final int index;

    protected boolean killed;

    protected ControlNode<?> control;

}
