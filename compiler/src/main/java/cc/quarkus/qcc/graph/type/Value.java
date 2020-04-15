package cc.quarkus.qcc.graph.type;

public interface Value<T extends Type<?>> {
    T getType();
}
