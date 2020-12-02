package cc.quarkus.qcc.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.BasicElement;
import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private ValueType type;
    private final BlockLabel blockLabel;
    private final HashMap<BasicBlock, Value> incomingValues = new HashMap<>();

    PhiValue(final int line, final int bci, final ValueType type, final BlockLabel blockLabel) {
        super(line, bci);
        this.type = type;
        this.blockLabel = blockLabel;
    }

    public Value getValueForBlock(final BasicBlock input) {
        return incomingValues.get(Assert.checkNotNullParam("input", input));
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
        if (incomingValues.containsKey(input)) {
            ctxt.error(element, this, "Phi already has a value for block %s", input);
            return;
        }
        incomingValues.put(input, value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final BasicElement element, final BlockLabel input, final Value value) {
        setValueForBlock(ctxt, element, BlockLabel.getTargetOf(input), value);
    }

    public boolean changeValueForBlock(final BasicBlock input, final Value oldVal, final Value newVal) {
        Assert.checkNotNullParam("input", input);
        return incomingValues.replace(input, Assert.checkNotNullParam("oldVal", oldVal), Assert.checkNotNullParam("newVal", newVal));
    }

    public Value removeValueForBlock(final BasicBlock input) {
        return incomingValues.remove(Assert.checkNotNullParam("input", input));
    }

    public Set<BasicBlock> incomingBlocks() {
        return incomingValues.keySet();
    }

    public Set<Map.Entry<BasicBlock, Value>> getIncomingValues() {
        return incomingValues.entrySet();
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

    int calcHashCode() {
        // every phi is globally unique
        return System.identityHashCode(this);
    }

    public boolean equals(final Object other) {
        // every phi is globally unique
        return this == other;
    }
}
