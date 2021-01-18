package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.Element;

/**
 * A literal is a value that was directly specified in a program.
 */
public abstract class Literal implements Value {
    Literal() {}

    public Element getElement() {
        return null;
    }

    public int getSourceLine() {
        return 0;
    }

    public int getBytecodeIndex() {
        return -1;
    }

    public final boolean equals(final Object obj) {
        return obj instanceof Literal && equals((Literal) obj);
    }

    public abstract boolean equals(Literal other);

    public abstract int hashCode();
}
