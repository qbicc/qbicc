package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface UnaryValue extends Value, ProgramNode {
    Value getInput();
    void setInput(Value input);
    Kind getKind();
    void setKind(Kind kind);

    default Type getType() {
        switch (getKind()) {
            case LENGTH_OF: return Type.S32;
            case SIZE_OF: return Type.U64;
            case NEGATE: return getInput().getType();
            case ADDRESS_OF: return ((NativeObjectType) getInput().getType()).getPointerType();
            default: throw Assert.impossibleSwitchCase(getKind());
        }
    }

    enum Kind {
        NEGATE,
        LENGTH_OF,
        SIZE_OF,
        ADDRESS_OF,
        ;
    }
}
