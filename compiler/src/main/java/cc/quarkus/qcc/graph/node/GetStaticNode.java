package cc.quarkus.qcc.graph.node;

import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.definition.FieldDefinition;
import cc.quarkus.qcc.type.definition.TypeDefinition;

public class GetStaticNode<V extends QType> extends AbstractNode<V> {

    public GetStaticNode(Graph<?> graph, ControlNode<?> control, FieldDefinition<V> field) {
        super(graph, control, field.getTypeDescriptor());
        this.field = field;
    }

    @Override
    public V getValue(Context context) {
        TypeDefinition cls = this.field.getTypeDefinition();
        return cls.getStatic(this.field);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return new ArrayList<>() {{
            add(getControl());
        }};
    }

    @Override
    public String label() {
        return "<getstatic:" + getId() + "> " + this.field;
    }

    @Override
    public String toString() {
        return label();
    }

    private final FieldDefinition<V> field;
}
