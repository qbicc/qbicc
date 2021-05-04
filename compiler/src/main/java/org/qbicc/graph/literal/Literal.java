package org.qbicc.graph.literal;

import org.qbicc.graph.Node;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

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

    /**
     * Determine if this literal is equal to zero, {@code null}, {@code false}, etc.
     *
     * @return {@code true} if the literal is zero, {@code false} otherwise
     */
    public abstract boolean isZero();

    public final boolean isNonZero() {
        return ! isZero();
    }

    public final boolean equals(final Object obj) {
        return obj instanceof Literal && equals((Literal) obj);
    }

    public abstract boolean equals(Literal other);

    public abstract int hashCode();

    Literal bitCast(LiteralFactory lf, final WordType toType) {
        return new BitCastLiteral(this, toType);
    }

    Literal convert(final LiteralFactory lf, final WordType toType) {
        return new ValueConvertLiteral(this, toType);
    }
}
