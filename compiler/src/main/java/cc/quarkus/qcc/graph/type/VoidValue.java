package cc.quarkus.qcc.graph.type;

public class VoidValue implements Value<VoidType, VoidValue> {

    public static final VoidValue VOID = new VoidValue();

    private VoidValue() {

    }

    @Override
    public VoidType getType() {
        return VoidType.INSTANCE;
    }
}
