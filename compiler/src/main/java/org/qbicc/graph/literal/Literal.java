package org.qbicc.graph.literal;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.qbicc.graph.Node;
import org.qbicc.graph.Unschedulable;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueVisitor;
import org.qbicc.type.WordType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A literal is a value that was directly specified in a program.
 */
public abstract class Literal implements Unschedulable, Value {
    private static final VarHandle bitCastLiteralsHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "bitCastLiterals", VarHandle.class, Literal.class, ImmutableMap.class);

    @SuppressWarnings({ "unused", "FieldMayBeFinal" })
    private volatile ImmutableMap<WordType, BitCastLiteral> bitCastLiterals = Maps.immutable.of();

    Literal() {}

    public Node callSite() {
        // no call site for literals
        return null;
    }

    public ExecutableElement element() {
        return null;
    }

    public int lineNumber() {
        return 0;
    }

    public int bytecodeIndex() {
        return -1;
    }

    /**
     * Determine if this literal is equal to zero, {@code null}, {@code false}, etc.
     *
     * @return {@code true} if the literal is zero, {@code false} otherwise
     */
    public abstract boolean isZero();

    public final boolean isNonZero() {
        return !isZero();
    }

    @Override
    public boolean isNullable() {
        return Value.super.isNullable();
    }

    public boolean isConstant() {
        return true;
    }

    public final boolean equals(final Object obj) {
        return obj instanceof Literal && equals((Literal) obj);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        return toString(b);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public abstract boolean equals(Literal other);

    public abstract int hashCode();

    Literal bitCast(LiteralFactory lf, final WordType toType) {
        ImmutableMap<WordType, BitCastLiteral> oldMap = bitCastLiterals;
        BitCastLiteral lit = oldMap.get(toType);
        if (lit != null) {
            return lit;
        }
        ImmutableMap<WordType, BitCastLiteral> newMap;
        lit = new BitCastLiteral(this, toType);
        for (;;) {
            newMap = oldMap.newWithKeyValue(toType, lit);
            newMap = caxBitCastLiterals(oldMap, newMap);
            if (oldMap == newMap) {
                return lit;
            }
            oldMap = newMap;
            if (oldMap.containsKey(toType)) {
                return oldMap.get(toType);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private ImmutableMap<WordType, BitCastLiteral> caxBitCastLiterals(ImmutableMap<WordType, BitCastLiteral> expected, ImmutableMap<WordType, BitCastLiteral> update) {
        return(ImmutableMap<WordType, BitCastLiteral>) bitCastLiteralsHandle.compareAndExchange(this, expected, update);
    }

    @Override
    public final <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return accept((LiteralVisitor<T, R>) visitor, param);
    }

    public abstract <T, R> R accept(LiteralVisitor<T, R> visitor, T param);
}
