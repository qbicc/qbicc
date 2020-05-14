package cc.quarkus.qcc.graph.node;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import cc.quarkus.qcc.graph.Graph;
import cc.quarkus.qcc.graph.type.MemoryToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.QType;
import cc.quarkus.qcc.type.descriptor.FieldDescriptor;
import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.descriptor.EphemeralTypeDescriptor;

public class PutFieldNode<V extends QType> extends AbstractNode<MemoryToken> {

    public PutFieldNode(Graph<?> graph, ControlNode<?> control, Node<ObjectReference> objRef, Node<V> val, FieldDescriptor<V> field, Node<MemoryToken> memory) {
        super(graph, control, EphemeralTypeDescriptor.MEMORY_TOKEN);
        this.objRef = objRef;
        this.val = val;
        this.field = field;
        this.memory = memory;
        objRef.addSuccessor(this);
        val.addSuccessor(this);
        memory.addSuccessor(this);
    }

    @Override
    public MemoryToken getValue(Context context) {
        ObjectReference objRef = context.get(this.objRef);
        V val = context.get(this.val);
        objRef.putField(this.field, val);
        return new MemoryToken();
    }

    @Override
    public List<? extends Node<?>> getPredecessors() {
        return List.of(getControl(), this.objRef, this.val, this.memory);
    }

    @Override
    public String label() {
        return "<putfield:" + getId() + "> " + this.field;
    }

    @Override
    public String toString() {
        return label();
    }

    private final Node<ObjectReference> objRef;

    private final Node<V> val;

    private final FieldDescriptor<V> field;

    private final Node<MemoryToken> memory;

    private AtomicReference<MemoryProjection> memoryOut = new AtomicReference<>();

}
