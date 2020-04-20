package cc.quarkus.qcc.graph.type;

public class NoneType implements ConcreteType<NoneType> {

    public static final NoneType INSTANCE = new NoneType();

    private NoneType() {

    }

    @Override
    public Type join(Type other) {
        return this;
    }
}
