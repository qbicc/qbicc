package cc.quarkus.qcc.graph;

import java.util.HashMap;
import java.util.Map;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

abstract class AbstractTerminator extends AbstractNode implements Terminator {
    private Map<PhiValue, Value> outboundValues = Map.of();

    AbstractTerminator(Node callSite, ExecutableElement element, int line, int bci) {
        super(callSite, element, line, bci);
    }

    Value getOutboundValue(PhiValue phi) {
        return outboundValues.get(phi);
    }

    boolean registerValue(PhiValue phi, Value val) {
        Map<PhiValue, Value> outboundValues = this.outboundValues;
        if (outboundValues.containsKey(phi)) {
            return false;
        }
        if (outboundValues.size() == 0) {
            this.outboundValues = Map.of(phi, val);
        } else if (outboundValues.size() == 1) {
            Map.Entry<PhiValue, Value> entry = outboundValues.entrySet().iterator().next();
            this.outboundValues = Map.of(entry.getKey(), entry.getValue(), phi, val);
        } else if (outboundValues.size() == 2) {
            this.outboundValues = new HashMap<>(outboundValues);
            this.outboundValues.put(phi, val);
        } else {
            this.outboundValues.put(phi, val);
        }
        return true;
    }

    Map<PhiValue, Value> getOutboundValues() {
        return outboundValues;
    }
}
