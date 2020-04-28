package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ObjectReference;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.TypeDescriptor;

public class NewNode extends AbstractNode<ObjectReference> {
    public NewNode(ControlNode<?> control, TypeDescriptor<ObjectReference> typeDescriptor) {
        super(control, typeDescriptor);
        this.typeDescriptor = typeDescriptor;
    }

    @Override
    public ObjectReference getValue(Context context) {
        ObjectReference objRef = new ObjectReference(((TypeDescriptor.ObjectTypeDescriptor)typeDescriptor).getTypeDefinition());
        return objRef;
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.singletonList(getControl());
    }

    @Override
    public String label() {
        return "<new> " + getType();
    }

    private final TypeDescriptor<ObjectReference> typeDescriptor;
}
