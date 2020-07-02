package cc.quarkus.qcc.graph;

/**
 * Unary operations.
 */
public interface UnaryValue extends Value {
    Value getInput();
    void setInput(Value input);
    Kind getKind();
    void setKind(Kind kind);

    default Type getType() {
        return getInput().getType();
    }

    enum Kind {
        NEGATE,
        ;
    }

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInput() : Util.throwIndexOutOfBounds(index);
    }
}
