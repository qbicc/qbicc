package cc.quarkus.qcc.graph.type;

import java.util.List;

public class StartType extends ControlType {

    public StartType(int maxLocals, int maxStack, List<ConcreteType<?>> paramTypes) {
        this.maxLocals = maxLocals;
        this.maxStack = maxStack;
        this.paramTypes = paramTypes;
        for ( ConcreteType<?> param : paramTypes) {
            System.err.println( "param: " + param);
        }
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
