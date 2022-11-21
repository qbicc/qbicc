package org.qbicc.graph.literal;

import static org.qbicc.graph.atomic.AccessModes.GlobalSeqCst;
import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.graph.ValueVisitorLong;
import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.VariableElement;

/**
 * A literal for some kind of variable.
 */
public abstract class VariableLiteral extends Literal {
    private final VariableElement variable;

    VariableLiteral(VariableElement variable) {
        this.variable = variable;
    }

    public VariableElement getVariableElement() {
        return variable;
    }

    @Override
    public PointerType getType() {
        return variable.getType().getPointer();
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean isNullable() {
        return false;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public boolean isReadable() {
        return true;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return toReferenceString(b);
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return null;
    }

    @Override
    public <T> long accept(ValueVisitorLong<T> visitor, T param) {
        return 0;
    }

    @Override
    public int hashCode() {
        return variable.hashCode();
    }

    @Override
    public AccessMode getDetectedMode() {
        if (variable.hasAllModifiersOf(ClassFile.ACC_VOLATILE)) {
            return GlobalSeqCst;
        } else {
            return SingleUnshared;
        }
    }

    @Override
    public final boolean equals(Literal other) {
        return other instanceof VariableLiteral vl && equals(vl);
    }

    public boolean equals(VariableLiteral other) {
        return this == other || other != null && variable.equals(other.variable);
    }
}
