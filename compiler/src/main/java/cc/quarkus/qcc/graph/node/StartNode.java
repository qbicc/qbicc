package cc.quarkus.qcc.graph.node;

import java.util.List;

import cc.quarkus.qcc.graph.type.ConcreteType;
import cc.quarkus.qcc.graph.type.StartType;
import cc.quarkus.qcc.parse.Frame;

public class StartNode extends ControlNode<StartType> {

    public StartNode(StartType initial) {
        super(initial, initial.getMaxLocals(), initial.getMaxStack());
        //Frame frame = new Frame(this,initial.getMaxLocals(), initial.getMaxStack());
        List<ConcreteType<?>> params = initial.getParamTypes();
        for (int i = 0; i < params.size(); ++i) {
            frame().store(i, projection(i, params.get(i)));
        }
        frame().io(new IOProjection(this));
        frame().memory(new MemoryProjection(this));
    }

    private <T extends ConcreteType<?>> VariableProjection<T> projection(int index, T type) {
        return new VariableProjection<>(this, type, index);
    }

    @Override
    public void addPredecessor(Node<?> in) {
        // no-op
    }

    @Override
    public String label() {
        return "<start>";
    }
}
