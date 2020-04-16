package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.BooleanValue;
import cc.quarkus.qcc.graph.type.Type;
import cc.quarkus.qcc.graph.type.Value;

import static cc.quarkus.qcc.graph.type.BooleanValue.of;

public enum CompareOp {

    EQUAL("=="),
    NOT_EQUAL("!="),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    GREATER_THAN(">"),
    GREATER_THAN_OR_EQUAL(">="),
    ;

    CompareOp(String label) {
        this.label = label;
    }

    public String label() {
        return this.label;
    }

    private final String label;
}
