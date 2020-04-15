package cc.quarkus.qcc.graph.type;

public abstract class AbstractValue<T extends Type<?>> implements Value<T>{

    protected AbstractValue(T type) {
        this.type = type;
    }

    @Override
    public T getType() {
        return this.type;
    }

    private final T type;
}
