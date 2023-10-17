package org.qbicc.graph.literal;

import org.qbicc.runtime.SafePointBehavior;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public abstract sealed class ExecutableLiteral extends Literal permits InitializerLiteral, InvokableLiteral {
    private final ExecutableElement element;

    ExecutableLiteral(ExecutableElement element) {
        this.element = element;
    }

    public ExecutableElement getExecutable() {
        return element;
    }

    @Override
    public final boolean equals(Literal other) {
        return other instanceof ExecutableLiteral el && equals(el);
    }

    public boolean equals(ExecutableLiteral other) {
        return this == other || other != null && element.equals(other.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    @Override
    public PointerType getType() {
        return element.getType().getPointer();
    }

    public InvokableType getPointeeType() {
        return element.getType();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean isPointeeConstant() {
        return true;
    }

    @Override
    public boolean isNoThrow() {
        return element.hasAllModifiersOf(ClassFile.I_ACC_NO_THROW);
    }

    @Override
    public SafePointBehavior safePointBehavior() {
        return element.safePointBehavior();
    }

    @Override
    public int safePointSetBits() {
        return element.safePointSetBits();
    }

    @Override
    public int safePointClearBits() {
        return element.safePointClearBits();
    }

    @Override
    public boolean isNoReturn() {
        return element.hasAllModifiersOf(ClassFile.I_ACC_NO_RETURN);
    }

    @Override
    public boolean isNoSideEffect() {
        return element.hasAllModifiersOf(ClassFile.I_ACC_NO_SIDE_EFFECTS);
    }

    @Override
    public boolean isFold() {
        return element.hasAllModifiersOf(ClassFile.I_ACC_FOLD);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return toReferenceString(b);
    }
}
