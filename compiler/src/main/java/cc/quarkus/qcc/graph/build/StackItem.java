package cc.quarkus.qcc.graph.build;

import cc.quarkus.qcc.graph.node.Node;
import cc.quarkus.qcc.type.QType;

public class StackItem {

    public <V extends QType> Node<V> load(Class<V> type) {
        return TypeUtil.checkType(this.value, type);
    }

    public <V extends QType> Node<V> get(Class<V> type) {
        if ( type == null ) {
            return (Node<V>) this.value;
        }
        return TypeUtil.checkType(this.value, type);
    }

    public void store(Node<?> val) {
        this.value = val;
    }

    private Node<?> value;
}
