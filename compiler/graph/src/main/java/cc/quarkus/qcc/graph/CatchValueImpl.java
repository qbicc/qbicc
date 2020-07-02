package cc.quarkus.qcc.graph;

/**
 *
 */
final class CatchValueImpl extends ValueImpl {
    public String getLabelForGraph() {
        return "catch value";
    }

    public Type getType() {
        // todo: exception type
        return Type.S32;
    }
}
