package cc.quarkus.qcc.graph.type;

public class ControlValue implements Value<ControlType> {
    @Override
    public ControlType getType() {
        return ControlType.INSTANCE;
    }
}
