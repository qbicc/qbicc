package cc.quarkus.qcc.graph;

import io.smallrye.common.constraint.Assert;

/**
 *
 */
public interface UnaryValue extends Value {
    Value getInput();
    void setInput(Value input);
    Kind getKind();
    void setKind(Kind kind);

    default Type getType() {
        switch (getKind()) {
            case LENGTH_OF: return Type.S32;
            case NEGATE: return getInput().getType();
            default: throw Assert.impossibleSwitchCase(getKind());
        }
    }

    enum Kind {
        NEGATE,
        LENGTH_OF,
        ;
    }

    default int getValueDependencyCount() {
        return 1;
    }

    default Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getInput() : Util.throwIndexOutOfBounds(index);
    }
}
