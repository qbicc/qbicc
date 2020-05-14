package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;

public class GetFieldNode<V extends QType> extends AbstractNode<V> {

    public GetFieldNode(Graph<?> graph, ControlNode<?> control, Node<ObjectReference> objRef, FieldDescriptor<V> field) {
        super(graph, control, field.getTypeDescriptor());
        this.objRef = objRef;
        this.field = field;
        objRef.addSuccessor(this);
    }

    @Override
    public V getValue(Context context) {
        ObjectReference objRef = context.get(this.objRef);
        return objRef.getField(this.field);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return List.of(getControl(), this.objRef);
    }

    @Override
    public String label() {
        return "<getfield:" + getId() + "> " + this.field;
    }

    private final Node<ObjectReference> objRef;
    private final FieldDescriptor<V> field;
}
