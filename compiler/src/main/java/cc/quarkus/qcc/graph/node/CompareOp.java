package cc.quarkus.qcc.graph.node;

import cc.quarkus.qcc.graph.type.BooleanValue;
import cc.quarkus.qcc.graph.type.IfValue;
import cc.quarkus.qcc.graph.type.NumericType;
import cc.quarkus.qcc.graph.type.NumericValue;
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

    public <C extends Comparable<C>> boolean execute(C lhsValue, C rhsValue) {
        int result = lhsValue.compareTo(rhsValue);
        switch ( this ) {
            case EQUAL:
                return result == 0;
            case NOT_EQUAL:
                return result != 0;
            case LESS_THAN:
                return result < 0;
            case LESS_THAN_OR_EQUAL:
                return result <= 0;
            case GREATER_THAN:
                return result > 0;
            case GREATER_THAN_OR_EQUAL:
                return result >= 0;
        }
        throw new UnsupportedOperationException("should not reach here");
    }

    private final String label;
}
