package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public class Clone extends AbstractValue implements UnaryValue {
    private final Node dependency;
    private final Value original;

    Clone(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value original) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.original = original;
    }

    int calcHashCode() {
        return dependency.hashCode() * 19 + original.hashCode();
    }

    public ValueType getType() {
        return original.getType();
    }

    public Value getInput() {
        return original;
    }

    public boolean equals(final Object other) {
        return other instanceof Clone && equals((Clone) other);
    }

    public boolean equals(final Clone other) {
        return this == other || other != null && dependency.equals(other.dependency) && original.equals(other.original);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? original : Util.throwIndexOutOfBounds(index);
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ValueVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
