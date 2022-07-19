package org.qbicc.graph;

import java.util.Set;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 * A handle for an inline assembly expression which can be called or invoked.
 */
public final class AsmHandle extends AbstractValueHandle {
    private final String instruction;
    private final String constraints;
    private final Set<Flag> flags;
    private final FunctionType type;

    AsmHandle(Node callSite, ExecutableElement element, int line, int bci, String instruction, String constraints, Set<Flag> flags, FunctionType type) {
        super(callSite, element, line, bci);
        this.instruction = instruction;
        this.constraints = constraints;
        this.flags = flags;
        this.type = type;
    }

    public boolean equals(final Object obj) {
        return obj instanceof AsmHandle other && equals(other);
    }

    public boolean equals(final AsmHandle other) {
        return this == other || other != null &&
            instruction.equals(other.instruction) &&
            constraints.equals(other.constraints) &&
            flags.equals(other.flags);
    }

    @Override
    int calcHashCode() {
        return 0;
    }

    public boolean isConstantLocation() {
        return false;
    }

    @Override
    public boolean isValueConstant() {
        return false;
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
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Asm";
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
        static final Flag[] VALUES = values();
    }
}
