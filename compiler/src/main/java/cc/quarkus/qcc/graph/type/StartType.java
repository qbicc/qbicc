package cc.quarkus.qcc.graph.type;

import java.util.List;

public class StartType implements Type<StartType> {

    public StartType(int maxLocals, int maxStack, List<ConcreteType<?>> paramTypes) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.paramTypes = paramTypes;
    }

    @Override
    public StartValue newInstance(Object... args) {
        for ( int i = 0 ; i < args.length ; ++i) {
            if ( ! ( args[i] instanceof Value ) ) {
                throw new IllegalArgumentException( "start must be created with Value<?> arguments");
            }
        }
        return newInstance((Value<? extends ConcreteType<?>,?>[]) args);
    }

    public StartValue newInstance(Value<? extends ConcreteType<?>,?>...args) {
        return new StartValue(this, args);
    }

    public int getMaxLocals() {
        return this.maxLocals;
    }

    public int getMaxStack() {
        return this.maxStack;
    }

    public List<ConcreteType<?>> getParamTypes() {
        return this.paramTypes;
    }

    private final List<ConcreteType<?>> paramTypes;

    private final int maxLocals;

    private final int maxStack;
}
