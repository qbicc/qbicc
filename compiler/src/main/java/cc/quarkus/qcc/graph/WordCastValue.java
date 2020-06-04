package cc.quarkus.qcc.graph;

/**
 *
 */
public interface WordCastValue extends CastValue {
    Kind getKind();
    void setKind(Kind kind);

    enum Kind {
        TRUNCATE,
        ZERO_EXTEND,
        SIGN_EXTEND,
    }

    static WordCastValue create(Value value, Type targetType) {
        WordCastValueImpl i = new WordCastValueImpl();
        i.setInput(value);
        i.setTargetType(targetType);
        return i;
    }
}
