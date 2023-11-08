package org.qbicc.graph.literal;

import java.util.EnumSet;
import java.util.Iterator;

import org.qbicc.machine.file.wasm.model.Insn;
import org.qbicc.machine.file.wasm.model.InsnSeq;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;

/**
 *
 */
public final class WasmLiteral extends Literal {
    private final InsnSeq insnSeq;
    private final FunctionType type;
    private final EnumSet<Flag> flags;
    private final int hashCode;

    WasmLiteral(InsnSeq insnSeq, FunctionType type, Flag... flags) {
        this.insnSeq = insnSeq.end();
        this.type = type;
        hashCode = insnSeq.instructions().hashCode();
        this.flags = flags.length == 0 ? EnumSet.noneOf(Flag.class) : EnumSet.of(flags[0], flags);
    }

    public InsnSeq insnSeq() {
        return insnSeq;
    }

    @Override
    public PointerType getType() {
        return type.getPointer();
    }

    @Override
    public FunctionType getPointeeType() {
        return type;
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof WasmLiteral wl && equals(wl);
    }

    public boolean equals(WasmLiteral other) {
        return this == other || other != null && insnSeq.instructions().equals(other.insnSeq.instructions()) && flags.equals(other.flags) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isNoSideEffect() {
        return ! flags.contains(Flag.SIDE_EFFECT);
    }

    @Override
    public boolean isNoReturn() {
        return flags.contains(Flag.NO_RETURN);
    }

    @Override
    public boolean isNoThrow() {
        return flags.contains(Flag.NO_THROW);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        b.append("wasm(");
        Iterator<Insn<?>> iterator = insnSeq.iterator();
        if (iterator.hasNext()) {
            b.append(iterator.next());
            while (iterator.hasNext()) {
                b.append(';');
                b.append(iterator.next());
            }
        }
        return b.append(')');
    }

    public enum Flag {
        // keep in order!
        /**
         * Explicitly declare that this assembly has a side effect.
         */
        SIDE_EFFECT,
        NO_THROW,
        NO_RETURN,
        ;
    }

}
