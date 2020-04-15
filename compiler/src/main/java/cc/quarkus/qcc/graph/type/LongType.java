package cc.quarkus.qcc.graph.type;

public class LongType implements ConcreteType<LongValue> {

    public static final LongType INSTANCE = new LongType();

    private LongType() {

    }

    @Override
    public LongValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Long.class);
        return new LongValue( (long) args[0]);
    }
}

