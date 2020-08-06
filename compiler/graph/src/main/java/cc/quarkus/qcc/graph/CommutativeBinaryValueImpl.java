package cc.quarkus.qcc.graph;

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

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }

    public String getLabelForGraph() {
        return kind.toString();
    }
}
