package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.ConcreteType;

public class ResultProjection<T extends ConcreteType<?>> extends Projection<CallNode<?>, T>{
    protected ResultProjection(CallNode<?> in, T outType) {
        super(in, outType);
    }

    @Override
    public String label() {
        return "<proj> result: " + getType().label();
    }
}
