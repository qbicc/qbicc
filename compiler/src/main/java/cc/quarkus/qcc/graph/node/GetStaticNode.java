package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.FieldDefinition;
import cc.quarkus.qcc.type.TypeDefinition;

public class GetStaticNode<V> extends AbstractNode<V> {

    public GetStaticNode(ControlNode<?> control, FieldDefinition<V> field) {
        super(control, field.getTypeDescriptor());
        this.field = field;
    }

    @Override
    public V getValue(Context context) {
        TypeDefinition cls = this.field.getTypeDefinition();
        return cls.getStatic(this.field);
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return null;
    }

    private final FieldDefinition<V> field;
}
