package cc.quarkus.qcc.graph.type;

public class CharType implements ConcreteType<Byte> {
    public static final CharType INSTANCE = new CharType();

    private CharType() {

    }
}
