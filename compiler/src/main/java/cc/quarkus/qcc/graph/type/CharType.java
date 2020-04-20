package cc.quarkus.qcc.graph.type;

public class CharType implements ConcreteType<CharType> {

    public static final CharType INSTANCE = new CharType();

    private CharType() {

    }

    @Override
    public CharValue newInstance(Object... args) {
        checkNewInstanceArguments(args, Character.class);
        return new CharValue((char) args[0]);
    }
}
