package cc.quarkus.qcc.graph;

/**
 * A return which returns a non-{@code void} value.
 */
public interface ValueReturn extends Return {
    Value getReturnValue();
    void setReturnValue(Value value);

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getReturnValue() : Util.throwIndexOutOfBounds(index);
    }
}
