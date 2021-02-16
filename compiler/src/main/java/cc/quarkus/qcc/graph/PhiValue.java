package cc.quarkus.qcc.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

public final class PhiValue extends AbstractValue implements PinnedNode {
    private ValueType type;
    private final BlockLabel blockLabel;
    private final HashMap<Terminator, Value> incomingValues = new HashMap<>();

    PhiValue(final Node callSite, final ExecutableElement element, final int line, final int bci, final ValueType type, final BlockLabel blockLabel) {
        super(callSite, element, line, bci);
        this.type = type;
        this.blockLabel = blockLabel;
    }

    public Value getValueForInput(final Terminator input) {
        return incomingValues.get(Assert.checkNotNullParam("input", input));
    }

    public void setValueForTerminator(final CompilationContext ctxt, final Element element, final Terminator input, final Value value) {
        Assert.checkNotNullParam("value", value);
        ValueType expected = getType();
        ValueType actual = value.getType();
        if (! actual.join(expected).equals(expected)) {
            ctxt.error(element, this, "Invalid input value for phi: expected %s, got %s (join is %s)", expected, actual, actual.join(expected));
        }
        if (incomingValues.containsKey(input)) {
            ctxt.error(element, this, "Phi already has a value for block %s", input.getTerminatedBlock());
            return;
        }
        incomingValues.put(input, value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BasicBlock input, final Value value) {
        setValueForTerminator(ctxt, element, input.getTerminator(), value);
    }

    public void setValueForBlock(final CompilationContext ctxt, final Element element, final BlockLabel input, final Value value) {
        setValueForBlock(ctxt, element, BlockLabel.getTargetOf(input), value);
    }

    public Set<Terminator> incomingTerminators() {
        return incomingValues.keySet();
    }

    public Set<Map.Entry<Terminator, Value>> getIncomingValues() {
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
