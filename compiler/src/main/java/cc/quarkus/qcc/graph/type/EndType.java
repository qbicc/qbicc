package cc.quarkus.qcc.graph.type;

public class EndType<T extends ConcreteType<?>> implements Type<EndValue> {

    public EndType(T returnType) {
        this.returnType = returnType;
    }

    @Override
    public EndValue newInstance(Object... args) {
        checkNewInstanceArguments(args, IOValue.class, MemoryValue.class, Value.class);
        return new EndValue(this, (IOValue) args[0], (MemoryValue) args[1], (Value) args[2]);
    }

    private final T returnType;
}
