package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.QType;

public abstract class Local {

    public Local(ControlNode<?> control, int index) {
        this.control = control;
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }

    public abstract void store(Node<?> val);

    public abstract <V extends QType> Node<V> load(Class<V> type);

    public abstract <V extends QType> Node<V> get(Class<V> type);

    public abstract Local duplicate();

    protected final int index;

    protected boolean killed;

    protected ControlNode<?> control;

}
