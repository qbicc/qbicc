package cc.quarkus.qcc.graph.node;

import java.util.Collections;
import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartType;
import cc.quarkus.qcc.graph.type.StartValue;
import cc.quarkus.qcc.graph.type.Value;
import cc.quarkus.qcc.interpret.Context;

public class StartNode extends AbstractControlNode<StartType, StartValue> {

    public StartNode(StartType initial) {
        super(initial, initial.getMaxLocals(), initial.getMaxStack());
        //Frame frame = new Frame(this,initial.getMaxLocals(), initial.getMaxStack());
        List<? extends ConcreteType<?>> params = initial.getParamTypes();
        for (int i = 0; i < params.size(); ++i) {
            storeProjection(i, params.get(i));
        }
        frame().io(new IOProjection(this));
        frame().memory(new MemoryProjection(this));
    }

    @Override
    public List<Node<?, ?>> getPredecessors() {
        return Collections.emptyList();
    }

    //private VariableProjection<T,?> projection(int index, T type) {

    private <T extends ConcreteType<T>, V extends Value<T,V>> void storeProjection(int index, ConcreteType<?> type) {
        VariableProjection<T, V> projection = new VariableProjection<T, V>(this, (T) type, index);
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
