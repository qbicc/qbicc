package cc.quarkus.qcc.graph;

/**
 * Unary operations.
 */
public interface UnaryValue extends Value {
    Value getInput();

    default Type getType() {
        return getInput().getType();
    }

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInput() : Util.throwIndexOutOfBounds(index);
    }
}
