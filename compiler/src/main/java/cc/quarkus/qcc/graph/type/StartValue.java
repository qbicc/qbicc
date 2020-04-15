package cc.quarkus.qcc.graph.type;

public class StartValue implements Value<StartType> {

    public StartValue(StartType type, Value<?>...arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public StartType getType() {
        return this.type;
    }

    private final Value<?>[] arguments;

    private final StartType type;
}
