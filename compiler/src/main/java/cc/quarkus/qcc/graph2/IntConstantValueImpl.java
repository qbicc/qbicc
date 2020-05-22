package cc.quarkus.qcc.graph2;

/**
 *
 */
final class IntConstantValueImpl extends ValueImpl {
    private final int value;

    IntConstantValueImpl(final int value) {
        this.value = value;
    }

    int getValue() {
        return value;
    }

    public String getLabelForGraph() {
        return "Int:" + value;
    }
}
