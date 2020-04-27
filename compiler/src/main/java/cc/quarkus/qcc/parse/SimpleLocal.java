package cc.quarkus.qcc.parse;

import cc.quarkus.qcc.graph.node.ControlNode;
import cc.quarkus.qcc.graph.node.Node;

public class SimpleLocal extends Local {

    public SimpleLocal(ControlNode<?> control, int index) {
        super(control, index);
    }

    @Override
    public void store(Node<?> val) {
        this.val = val;
        this.killed = true;
    }

    @Override
    public <V> Node<V> load(Class<V> type) {
        return TypeUtil.checkType(this.val, type);
    }

    public <V> Node<V> get(Class<V> type) {
        return load(type);
    }

    public String toString() {
        return "Local: val=" + val;
    }

    @Override
    public Local duplicate() {
        cc.quarkus.qcc.parse.SimpleLocal dupe = new cc.quarkus.qcc.parse.SimpleLocal(this.control, this.index);
        dupe.val = this.val;
        return dupe;
    }

    protected Node<?> val;
}
