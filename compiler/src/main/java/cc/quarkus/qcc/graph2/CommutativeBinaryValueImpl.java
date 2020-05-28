package cc.quarkus.qcc.graph2;

/**
 *
 */
final class CommutativeBinaryValueImpl extends BinaryValueImpl implements CommutativeBinaryValue {
    Kind kind;

    CommutativeBinaryValueImpl() {
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
