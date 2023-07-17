package org.qbicc.graph;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.qbicc.context.ProgramLocatable;

abstract class AbstractTerminator extends AbstractNode implements Terminator {
    private final Map<Slot, Value> targetValues;

    AbstractTerminator(ProgramLocatable pl) {
        this(pl, Map.of());
    }

    AbstractTerminator(ProgramLocatable pl, Map<Slot, Value> targetValues) {
        super(pl);
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
