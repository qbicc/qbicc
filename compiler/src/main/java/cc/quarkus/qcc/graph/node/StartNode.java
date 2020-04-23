package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.StartToken;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.TypeDescriptor;

public class StartNode extends AbstractControlNode<StartToken> {

    public StartNode(MethodDescriptor descriptor, int maxLocals, int maxStack) {
        super(TypeDescriptor.EphemeralTypeDescriptor.START_TOKEN, maxLocals, maxStack);
        List<TypeDescriptor<?>> params = descriptor.getParamTypes();
        for (int i = 0; i < params.size(); ++i) {
            storeProjection(i, params.get(i));
        }
        frame().io(new IOProjection(this));
        frame().memory(new MemoryProjection(this));
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.emptyList();
    }

    private <V> void storeProjection(int index, TypeDescriptor<V> type) {
        ParameterProjection<V> projection = new ParameterProjection<V>(this, type, index);
        frame().store(index, projection);
    }

    @Override
    public StartToken getValue(Context context) {
        return context.get(this);
    }

    @Override
    public String label() {
        return "<start>";
    }
}
