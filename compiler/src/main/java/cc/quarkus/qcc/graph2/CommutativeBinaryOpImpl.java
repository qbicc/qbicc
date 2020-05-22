package cc.quarkus.qcc.graph2;

/**
 *
 */
final class CommutativeBinaryOpImpl extends BinaryOpImpl implements CommutativeBinaryOp {
    Kind kind;

    CommutativeBinaryOpImpl() {
    }

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
