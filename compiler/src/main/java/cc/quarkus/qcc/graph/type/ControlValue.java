package cc.quarkus.qcc.graph.type;

public final class ControlValue implements Value<ControlType,ControlValue> {
    @Override
    public ControlType getType() {
        return ControlType.INSTANCE;
    }
}
