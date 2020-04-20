package cc.quarkus.qcc.graph.type;

public class ByteType implements ConcreteType<ByteType> {
    public static final ByteType INSTANCE = new ByteType();

    private ByteType() {

    }

    @Override
    public ByteValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Byte.class);
        return new ByteValue((Byte) args[0]);
    }
}
