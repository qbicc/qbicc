package cc.quarkus.qcc.graph.type;

public class BooleanType implements ConcreteType<BooleanValue> {
    public static final BooleanType INSTANCE = new BooleanType();

    private BooleanType() {

    }

    @Override
    public BooleanValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Boolean.class);
        if ((Boolean) args[0]) {
            return BooleanValue.TRUE;
        }
        return BooleanValue.FALSE;
    }
}
