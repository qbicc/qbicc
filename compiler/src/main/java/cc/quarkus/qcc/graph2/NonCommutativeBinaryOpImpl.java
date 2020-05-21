package cc.quarkus.qcc.graph2;

/**
 *
 */
final class NonCommutativeBinaryOpImpl extends BinaryOpImpl implements NonCommutativeBinaryOp {
    Kind kind;

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
    }

    public String getLabelForGraph() {
        return kind.toString();
    }
}
