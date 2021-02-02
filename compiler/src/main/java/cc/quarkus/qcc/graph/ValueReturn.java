package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * A return which returns a non-{@code void} value.
 */
public final class ValueReturn extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value returnValue;

    private final BasicBlock terminatedBlock;

    ValueReturn(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final Value returnValue) {
        super(callSite, element, line, bci);
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
        return Objects.hash(ValueReturn.class, dependency, returnValue);
    }

    public boolean equals(final Object other) {
        return other instanceof ValueReturn && equals((ValueReturn) other);
    }

    public boolean equals(final ValueReturn other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && returnValue.equals(other.returnValue);
    }
}
