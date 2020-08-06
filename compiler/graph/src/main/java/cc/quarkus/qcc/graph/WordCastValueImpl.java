package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

final class WordCastValueImpl extends CastValueImpl implements WordCastValue {
    Kind kind;

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        Assert.checkNotNullParam("kind", kind);
        this.kind = kind;
    }

    public <P> void accept(GraphVisitor<P> visitor, P param) {
        visitor.visit(param, this);
    }
}
