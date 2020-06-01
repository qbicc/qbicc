package cc.quarkus.qcc.graph2;

/**
 * TEMPORARY
 */
final class LongConstantValueImpl extends ValueImpl {
    private final long value;

    LongConstantValueImpl(final long value) {
        this.value = value;
    }

    long getValue() {
        return value;
    }

    public String getLabelForGraph() {
        return "Long:" + value;
    }
}
