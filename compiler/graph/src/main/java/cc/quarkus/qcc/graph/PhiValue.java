package cc.quarkus.qcc.graph;

import java.util.Map;

import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode, Value {
    private final Key key = new Key();
    private final Type type;
    private final BlockLabel blockLabel;

    PhiValue(final Type type, final BlockLabel blockLabel) {
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

    public Type getType() {
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
