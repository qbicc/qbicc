package org.qbicc.graph;

import java.util.LinkedHashMap;
import java.util.Map;

import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.type.definition.element.ExecutableElement;

abstract class AbstractTerminator extends AbstractNode implements Terminator {
    private Map<PhiValue, Value> outboundValues = Map.of();

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci) {
        super(callSite, element, line, bci);
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
}
