package cc.quarkus.qcc.graph.type;

public class ShortType implements ConcreteType<ShortValue> {

    public static final ShortType INSTANCE = new ShortType();

    private ShortType() {

    }

    @Override
    public ShortValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Short.class);
        return new ShortValue((short) args[0]);
    }
}
