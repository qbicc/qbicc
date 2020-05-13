package cc.quarkus.qcc.graph.node;

import java.util.ListIterator;
import java.util.Set;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public abstract class AbstractControlNode<V extends QType> extends AbstractNode<V> implements ControlNode<V> {

    protected AbstractControlNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<V> outType) {
        super(graph, control, outType);
    }

    public AbstractControlNode(Graph<?> graph, TypeDescriptor<V> outType) {
        super(graph, outType);
    }
}
