package cc.quarkus.qcc.graph;

/**
 * An instruction which throws an exception <em>to the caller</em>.  To throw an exception to a catch block,
 * see {@link TryThrow}.
 */
public interface Throw extends Terminator {
    Value getThrownValue();
    void setThrownValue(Value thrown);
}
