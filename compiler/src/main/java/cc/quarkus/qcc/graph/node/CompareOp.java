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

    /*
    public <T extends Value<?> & Comparable<T>> BooleanValue compare(T lhsValue, T rhsValue) {
        int result = lhsValue.compareTo(rhsValue);
        System.err.println( "COMPOP " + lhsValue + " " + this + " " + rhsValue);
        switch (this) {
            case EQUAL:
                return of(result == 0);
            case NOT_EQUAL:
                return of(result != 0);
            case LESS_THAN:
                return of(result < 0);
            case LESS_THAN_OR_EQUAL:
                return of(result <= 0);
            case GREATER_THAN:
                return of(result > 0);
            case GREATER_THAN_OR_EQUAL:
                return of(result >= 0);
        }
        return BooleanValue.FALSE;
    }
     */

    private final String label;
}
