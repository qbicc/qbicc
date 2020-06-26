package cc.quarkus.qcc.graph;

/**
 * A {@code new} allocation operation for arrays.
 */
public interface NewArrayValue extends Value, MemoryState, GraphFactory.MemoryStateValue {
    ArrayClassType getType();

    void setType(ArrayClassType type);

    Value getSize();

    void setSize(Value size);

    static NewArrayValue create(ArrayClassType classType, Value size) {
        NewArrayValueImpl value = new NewArrayValueImpl();
        value.setType(classType);
        value.setSize(size);
        return value;
    }

    default Value getValue() {
        return this;
    }

    default MemoryState getMemoryState() {
        return this;
    }
}
