package cc.quarkus.qcc.graph.type;

public class VoidType implements ConcreteType<VoidValue> {

    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {

    }

    @Override
    public VoidValue newInstance(Object... args) {
        return VoidValue.VOID;
    }
}
