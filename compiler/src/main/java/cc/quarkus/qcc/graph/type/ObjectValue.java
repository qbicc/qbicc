package cc.quarkus.qcc.graph.type;

public class ObjectValue implements Value<ObjectType, ObjectValue> {

    public ObjectValue(ObjectType type, Object...arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    @Override
    public ObjectType getType() {
        return this.type;
    }

    private final ObjectType type;
    private final Object[] arguments;
}
