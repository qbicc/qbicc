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
        VALUE_CONVERT,
    }

    static WordCastValue create(Value value, Kind kind, Type targetType) {
        WordCastValueImpl i = new WordCastValueImpl();
        i.setInput(value);
        i.setKind(kind);
        i.setType(targetType);
        return i;
    }
}
