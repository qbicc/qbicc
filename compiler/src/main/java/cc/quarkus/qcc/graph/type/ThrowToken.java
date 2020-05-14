package cc.quarkus.qcc.graph.type;

import cc.quarkus.qcc.type.ObjectReference;
import cc.quarkus.qcc.type.QType;

public class ThrowToken implements QType, ThrowSource {

    public ThrowToken(ObjectReference throwValue) {
        this.throwValue = throwValue;
    }

    @Override
    public ObjectReference getThrowValue() {
        return this.throwValue;
    }

    @Override
    public String toString() {
        return "ThrowToken{" +
                "thrown=" + throwValue +
                '}';
    }

    private final ObjectReference throwValue;
}
