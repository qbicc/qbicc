package cc.quarkus.qcc.graph.node;

public enum CompareOp {

    EQUAL("=="),
    NOT_EQUAL( "!="),
    LESS_THAN( "<"),
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
