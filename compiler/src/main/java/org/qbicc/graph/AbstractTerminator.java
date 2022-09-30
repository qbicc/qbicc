package org.qbicc.graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractTerminator extends AbstractNode implements Terminator {
    private Map<PhiValue, Value> outboundValues = Map.of();
    private final Map<Slot, Value> targetValues;

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci) {
        this(callSite, element, line, bci, Map.of());
    }

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci, Map<Slot, Value> targetValues) {
        super(callSite, element, line, bci);
        this.targetValues = targetValues;
    }

    @Override
    public Value getOutboundValue(PhiValue phi) {
        Value value = outboundValues.get(phi);
        LiteralFactory lf = getElement().getEnclosingType().getContext().getLiteralFactory();
        return value != null ? value : lf.undefinedLiteralOfType(phi.getType());
    }

    @Override
    public boolean registerValue(PhiValue phi, Value val) {
        Map<PhiValue, Value> outboundValues = this.outboundValues;
        if (outboundValues.containsKey(phi)) {
            // an exactly duplicate registration is OK
            return outboundValues.get(phi) == val;
        }
        if (outboundValues.size() == 0) {
            this.outboundValues = Map.of(phi, val);
        } else if (outboundValues.size() == 1) {
            Map.Entry<PhiValue, Value> entry = outboundValues.entrySet().iterator().next();
            this.outboundValues = Map.of(entry.getKey(), entry.getValue(), phi, val);
        } else if (outboundValues.size() == 2) {
            this.outboundValues = new LinkedHashMap<>(outboundValues);
            this.outboundValues.put(phi, val);
        } else {
            this.outboundValues.put(phi, val);
        }
        return true;
    }

    @Override
    public Map<PhiValue, Value> getOutboundValues() {
        return outboundValues;
    }

    @Override
    public Value getOutboundArgument(Slot slot) throws NoSuchElementException {
        Value value = targetValues.get(slot);
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    @Override
    public Set<Slot> getOutboundArgumentNames() {
        return targetValues.keySet();
    }
}
