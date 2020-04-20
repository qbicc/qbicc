package cc.quarkus.qcc.graph.type;

public class EndType<T extends ConcreteType<T>>  implements Type<EndType<T>> {

    public EndType(T returnType) {
        this.returnType = returnType;
    }

    @Override
    public EndValue<T> newInstance(Object... args) {
        return null;
    }

    /*
    @Override
    public EndValue<T> newInstance(Object... args) {
        checkNewInstanceArguments(args, IOValue.class, MemoryValue.class, Value.class);
        return new EndValue<T>(this, (IOValue) args[0], (MemoryValue) args[1], (Value<T,?>) args[2]);
    }

     */

    private final T returnType;
}
