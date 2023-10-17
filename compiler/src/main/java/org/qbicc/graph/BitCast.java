package org.qbicc.graph;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.WordType;

/**
 *
 */
public final class BitCast extends AbstractWordCastValue {
    BitCast(final ProgramLocatable pl, final Value value, final WordType toType) {
        super(pl, value, toType);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    @Override
    boolean equals(AbstractWordCastValue other) {
        return other instanceof BitCast bc && equals(bc);
    }

    boolean equals(BitCast other) {
        return super.equals(other);
    }

    @Override
    public boolean isNoThrow() {
        return getInput().isNoThrow();
    }

    @Override
    public SafePointBehavior safePointBehavior() {
        return getInput().safePointBehavior();
    }

    @Override
    public int safePointSetBits() {
        return getInput().safePointSetBits();
    }

    @Override
    public int safePointClearBits() {
        return getInput().safePointClearBits();
    }

    @Override
    public boolean isNoReturn() {
        return getInput().isNoReturn();
    }

    @Override
    public boolean isNoSideEffect() {
        return getInput().isNoSideEffect();
    }

    @Override
    String getNodeName() {
        return "BitCast";
    }
}
