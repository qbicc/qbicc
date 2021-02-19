package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * An instruction which throws an exception <em>to the caller</em>.  To throw an exception to a catch block,
 * use {@link Goto}.
 */
public final class Throw extends AbstractTerminator implements Terminator {
    private final Node dependency;
    private final Value thrownValue;
    private final BasicBlock terminatedBlock;

    Throw(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final Value thrownValue) {
        super(callSite, element, line, bci);
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

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(Throw.class, dependency, thrownValue);
    }

    public boolean equals(final Object other) {
        return other instanceof Throw && equals((Throw) other);
    }

    public boolean equals(final Throw other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && thrownValue.equals(other.thrownValue);
    }
}
