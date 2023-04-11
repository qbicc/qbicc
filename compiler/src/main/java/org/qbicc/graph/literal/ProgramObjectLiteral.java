package org.qbicc.graph.literal;

import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramObject;
import org.qbicc.type.PointerType;

/**
 * A literal referring to some program object.
 */
public final class ProgramObjectLiteral extends Literal {
    private final ProgramObject programObject;

    ProgramObjectLiteral(ProgramObject programObject) {
        this.programObject = programObject;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('@').append(programObject.getName());
    }

    @Override
    public PointerType getType() {
        return programObject.getSymbolType();
    }

    public String getName() {
        return programObject.getName();
    }

    /**
     * Get the program object corresponding to this literal.
     *
     * @return the program object (not {@code null})
     */
    public ProgramObject getProgramObject() {
        return programObject;
    }

    @Override
    public <T, R> R accept(LiteralVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean isPointeeConstant() {
        return programObject instanceof Data d && d.isConstant()
            || programObject instanceof Function
            || programObject instanceof FunctionDeclaration /* fd && ! fd.isWeak() */;
    }

    @Override
    public boolean isPointeeNullable() {
        return ! (programObject instanceof Data d && d.isConstant() && d.getValue().isNullable());
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof ProgramObjectLiteral pol && equals(pol);
    }

    public boolean equals(ProgramObjectLiteral other) {
        return this == other || other != null && programObject.equals(other.programObject);
    }

    @Override
    public int hashCode() {
        return programObject.hashCode();
    }
}
