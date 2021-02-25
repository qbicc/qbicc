package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

public final class Cmp extends AbstractBinaryValue implements CommutativeBinaryValue {
    private final SignedIntegerType integerType;

    Cmp(final Node callSite, final ExecutableElement element, final int line, final int bci, final Value v1, final Value v2, SignedIntegerType integerType) {
        super(callSite, element, line, bci, v1, v2);
        this.integerType = integerType;
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public SignedIntegerType getType() {
        return integerType;
    }
}
