package cc.quarkus.qcc.plugin.native_;

import cc.quarkus.qcc.graph.literal.Literal;

final class ConstantInfo {
    final boolean defined;
    final Literal value;

    ConstantInfo(final boolean defined, final Literal value) {
        this.defined = defined;
        this.value = value;
    }
}
