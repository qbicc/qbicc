package cc.quarkus.qcc.graph;

import java.util.Map;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private final Key key = new Key();
    private ValueType type;
    private final BlockLabel blockLabel;

    PhiValue(final ValueType type, final BlockLabel blockLabel) {
        super(0, -1);
        this.type = type;
        this.blockLabel = blockLabel;
    }

    public Value getValueForBlock(final BasicBlock input) {
        return input.outboundValues.get(key);
    }

    public void setValueForBlock(final CompilationContext ctxt, final BasicElement element, final BasicBlock input, final Value value) {
        Assert.checkNotNullParam("value", value);
        if (! value.getType().equals(getType())) {
            try {
                type = value.getType().join(getType());
            } catch (IllegalArgumentException e) {
                ctxt.error(element, this, "Failed to derive type for phi: %s", e.getMessage());
            }
        }
        Map<Key, Value> ov = input.outboundValues;
        if (ov.containsKey(key)) {
            ctxt.error(element, this, "Phi already has a value for block %s", input);
            return;
        }
        input.outboundValues = Util.mapWithEntry(ov, key, value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final BasicElement element, final BlockLabel input, final Value value) {
        setValueForBlock(ctxt, element, BlockLabel.getTargetOf(input), value);
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
