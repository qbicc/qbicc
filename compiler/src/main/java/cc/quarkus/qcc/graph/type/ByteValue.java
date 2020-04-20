package cc.quarkus.qcc.graph.type;

public class ByteValue implements Value<ByteType, ByteValue> {

    public ByteValue(byte val) {
        this.val = val;
    }

    @Override
    public ByteType getType() {
        return ByteType.INSTANCE;
    }

    private final byte val;
}
