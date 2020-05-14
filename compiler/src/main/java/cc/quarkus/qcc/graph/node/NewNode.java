package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.interpret.InterpreterHeap;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.descriptor.ObjectTypeDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

public class NewNode extends AbstractNode<ObjectReference> {
    public NewNode(Graph<?> graph, ControlNode<?> control, TypeDescriptor<ObjectReference> typeDescriptor) {
        super(graph, control, typeDescriptor);
        this.typeDescriptor = typeDescriptor;
    }

    @Override
    public ObjectReference getValue(Context context) {
        InterpreterHeap heap = context.thread().heap();
        return heap.newObject(((ObjectTypeDescriptor) typeDescriptor).getTypeDefinition());
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return List.of(getControl());
    }

    @Override
    public String label() {
        return "<new> " + getType();
    }

    private final TypeDescriptor<ObjectReference> typeDescriptor;
}
