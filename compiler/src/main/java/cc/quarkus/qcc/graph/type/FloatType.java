package cc.quarkus.qcc.graph.type;

public class FloatType implements ConcreteType<FloatType> {

    public static final FloatType INSTANCE = new FloatType();

    private FloatType() {

    }


    @Override
    public FloatValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Float.class);
        return new FloatValue((float) args[0]);
    }
}
