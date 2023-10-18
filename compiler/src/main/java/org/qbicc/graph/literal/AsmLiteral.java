package org.qbicc.graph.literal;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import java.util.Objects;
import java.util.Set;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;

/**
 * A literal for an inline assembly expression which can be called or invoked.
 */
public final class AsmLiteral extends Literal {
    private final String instruction;
    private final String constraints;
    private final Set<Flag> flags;
    private final FunctionType type;
    private final int hashCode;

    AsmLiteral(String instruction, String constraints, Set<Flag> flags, FunctionType type) {
        this.instruction = instruction;
        this.constraints = constraints;
        this.flags = flags;
        this.type = type;
        hashCode = Objects.hash(instruction, constraints, flags, type);
    }

    public boolean equals(final Literal obj) {
        return obj instanceof AsmLiteral other && equals(other);
    }

    public boolean equals(final AsmLiteral other) {
        return this == other || other != null &&
            instruction.equals(other.instruction) &&
            constraints.equals(other.constraints) &&
            flags.equals(other.flags) &&
            type.equals(other.type);
    }

    public boolean hasFlag(Flag flag) {
        return flags.contains(flag);
    }

    public Set<Flag> getFlags() {
        return flags;
    }

    public String getInstruction() {
        return instruction;
    }

    public String getConstraints() {
        return constraints;
    }

    @Override
    public boolean isNoThrow() {
        return flags.contains(Flag.NO_THROW);
    }

    @Override
    public boolean isNoReturn() {
        return flags.contains(Flag.NO_RETURN);
    }

    @Override
    public boolean isNoSideEffect() {
        return ! (flags.contains(Flag.SIDE_EFFECT) || flags.contains(Flag.IMPLICIT_SIDE_EFFECT));
    }

    @Override
    public SafePointBehavior safePointBehavior() {
        return SafePointBehavior.ALLOWED;
    }

    @Override
    public boolean isZero() {
        return false;
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
    public StringBuilder toString(StringBuilder b) {
        return type.toString(b.append("asm \"").append(instruction).append("\", \"").append(constraints).append("\" as "));
    }

    @Override
    public FunctionType getPointeeType() {
        return type;
    }

    @Override
    public PointerType getType() {
        // not really allowed though
        return type.getPointer();
    }

    @Override
    public AccessMode getDetectedMode() {
        return SingleUnshared;
    }

    public enum Flag {
        INTEL_DIALECT,
        ALIGN_STACK,
        /**
         * Explicitly declare that this assembly has a side effect not represented in the constraint list operand.
         */
        SIDE_EFFECT,
        /**
         * Declare that this assembly's constraint list expresses some implicit side effect.
         */
        IMPLICIT_SIDE_EFFECT,
        NO_THROW,
        NO_RETURN,
        ;
    }
}
