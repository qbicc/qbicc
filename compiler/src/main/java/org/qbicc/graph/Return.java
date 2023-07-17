package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 * A return from an invokable program element.
 */
public final class Return extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value returnValue;

    private final BasicBlock terminatedBlock;

    Return(final ProgramLocatable pl, final BlockEntry blockEntry, final Node dependency, final Value returnValue) {
        super(pl);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.returnValue = returnValue;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getReturnValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Return.class, dependency, returnValue);
    }

    @Override
    String getNodeName() {
        return "Return";
    }

    public boolean equals(final Object other) {
        return other instanceof Return && equals((Return) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        returnValue.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final Return other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && returnValue.equals(other.returnValue);
    }
}
