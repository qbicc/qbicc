package org.qbicc.graph;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractTerminator extends AbstractNode implements Terminator {
    private final Map<Slot, Value> targetValues;

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci) {
        this(callSite, element, line, bci, Map.of());
    }

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci, Map<Slot, Value> targetValues) {
        super(callSite, element, line, bci);
        targetValues.forEach((s, v) -> {
            if (v == null) {
                throw new IllegalArgumentException("Null value given for slot " + s);
            }
        });
        this.targetValues = targetValues;
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
