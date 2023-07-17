package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 * An instruction which throws an exception <em>to the caller</em>.  To throw an exception to a catch block,
 * use {@link Goto}.
 */
public final class Throw extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value thrownValue;
    private final BasicBlock terminatedBlock;

    Throw(final ProgramLocatable pl, final BlockEntry blockEntry, final Node dependency, final Value thrownValue) {
        super(pl);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.thrownValue = thrownValue;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    public Value getThrownValue() {
        return thrownValue;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? thrownValue : Util.throwIndexOutOfBounds(index);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public boolean maySafePoint() {
        return false;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Throw.class, dependency, thrownValue);
    }

    @Override
    String getNodeName() {
        return "Throw";
    }

    public boolean equals(final Object other) {
        return other instanceof Throw && equals((Throw) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        thrownValue.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final Throw other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && thrownValue.equals(other.thrownValue);
    }
}
