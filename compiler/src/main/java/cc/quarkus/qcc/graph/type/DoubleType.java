package cc.quarkus.qcc.graph.type;

public class DoubleType implements ConcreteType<DoubleValue> {

    public static final DoubleType INSTANCE = new DoubleType();

    private DoubleType() {

    }

    @Override
    public DoubleValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Double.class);
        return new DoubleValue( (double) args[0]);
    }
}
