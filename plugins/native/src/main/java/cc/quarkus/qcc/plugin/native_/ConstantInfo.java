package org.qbicc.plugin.native_;

import org.qbicc.graph.literal.Literal;

final class ConstantInfo {
    final boolean defined;
    final Literal value;

    ConstantInfo(final boolean defined, final Literal value) {
        this.defined = defined;
        this.value = value;
    }
}
