package cc.quarkus.qcc.graph;

import java.util.Map;

import cc.quarkus.qcc.type.ValueType;
import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private final Key key = new Key();
    private final ValueType type;
    private final BlockLabel blockLabel;

    PhiValue(final ValueType type, final BlockLabel blockLabel) {
        this.type = type;
        this.blockLabel = blockLabel;
    }

    public Value getValueForBlock(final BasicBlock input) {
        return input.outboundValues.get(key);
    }

    public void setValueForBlock(final BasicBlock input, final Value value) {
        Assert.checkNotNullParam("value", value);
        if (value.getType() != getType()) {
            throw new IllegalStateException("Phi type mismatch");
        }
        Map<Key, Value> ov = input.outboundValues;
        if (ov.containsKey(key)) {
            throw new IllegalStateException("Phi " + this + " already has a value for block " + input);
        }
        input.outboundValues = Util.mapWithEntry(ov, key, value);
    }

    public void setValueForBlock(final BlockLabel input, final Value value) {
        setValueForBlock(BlockLabel.getTargetOf(input), value);
    }

    public ValueType getType() {
        return type;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public BlockLabel getPinnedBlockLabel() {
        return blockLabel;
    }

    static final class Key {}
}
