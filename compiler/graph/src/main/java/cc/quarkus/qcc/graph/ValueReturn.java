package cc.quarkus.qcc.graph;

/**
 * A return which returns a non-{@code void} value.
 */
public interface ValueReturn extends Return {
    Value getReturnValue();
    void setReturnValue(Value value);
}
