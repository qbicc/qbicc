package cc.quarkus.qcc.graph.type;

public class CharValue implements Value<CharType, CharValue> {

    public CharValue(char val) {
        this.val = val;
    }

    @Override
    public CharType getType() {
        return CharType.INSTANCE;
    }

    private final char val;
}
