package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.interpret.Context;
import cc.quarkus.qcc.type.MethodDescriptor;
import cc.quarkus.qcc.type.MethodDescriptorImpl;
import cc.quarkus.qcc.type.TypeDescriptor;

public class StartNode extends AbstractControlNode<StartValue> {

    public StartNode(MethodDescriptor descriptor, int maxLocals, int maxStack) {
        super(StartValue.class, maxLocals, maxStack);
        //Frame frame = new Frame(this,initial.getMaxLocals(), initial.getMaxStack());
        List<TypeDescriptor<?>> params = descriptor.getParamTypes();
        for (int i = 0; i < params.size(); ++i) {
            storeProjection(i, params.get(i).valueType());
        }
        frame().io(new IOProjection(this));
        frame().memory(new MemoryProjection(this));
    }

    @Override
    public List<Node<?>> getPredecessors() {
        return Collections.emptyList();
    }

    //private VariableProjection<T,?> projection(int index, T type) {

    private <V> void storeProjection(int index, Class<V> type) {
        VariableProjection<V> projection = new VariableProjection<V>(this, type, index);
        frame().store(index, projection);
    }

    @Override
    public StartValue getValue(Context context) {
        return context.get(this);
    }

    @Override
    public String label() {
        return "<start>";
    }
}
