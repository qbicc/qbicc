package cc.quarkus.qcc.graph;

/**
 *
 */
public interface CastValue extends Value, ProgramNode {
    Value getInput();
    void setInput(Value value);

    Type getType();
    void setType(Type targetType);

    static CastValue create(Value value, Type targetType) {

        CastValueImpl i = new CastValueImpl();
        i.setInput(value);
        i.setType(targetType);
        return i;
    }
}
