package cc.quarkus.qcc.graph2;

/**
 *
 */
public interface CastValue extends Value, ProgramNode {
    Value getInput();
    void setInput(Value value);

    Type getTargetType();
    void setTargetType(Type targetType);

    static CastValue create(Value value, Type targetType) {

        CastValueImpl i = new CastValueImpl();
        i.setInput(value);
        i.setTargetType(targetType);
        return i;
    }
}
