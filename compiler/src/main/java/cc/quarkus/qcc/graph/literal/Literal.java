package cc.quarkus.qcc.graph.literal;

import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.Unschedulable;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A literal is a value that was directly specified in a program.
 */
public abstract class Literal implements Unschedulable, Value {
    Literal() {}

    public Node getCallSite() {
        // no call site for literals
        return null;
    }

    public ExecutableElement getElement() {
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
