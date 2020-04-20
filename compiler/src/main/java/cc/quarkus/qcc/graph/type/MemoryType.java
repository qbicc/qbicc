package cc.quarkus.qcc.graph.type;

public class MemoryType implements Type<MemoryType> {
    public static final MemoryType INSTANCE = new MemoryType();

    private MemoryType() {

    }

    @Override
    public MemoryValue newInstance(Object... args) {
        checkNewInstanceArguments(args);
        return new MemoryValue();
    }
}
