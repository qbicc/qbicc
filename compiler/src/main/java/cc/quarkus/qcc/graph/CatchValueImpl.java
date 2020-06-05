package cc.quarkus.qcc.graph;

/**
 *
 */
final class CatchValueImpl extends ValueProgramNodeImpl {
    public String getLabelForGraph() {
        return "catch value";
    }

    public Type getType() {
        // todo: exception type
        return Type.S32;
    }
}
