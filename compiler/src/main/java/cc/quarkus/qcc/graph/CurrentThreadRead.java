package cc.quarkus.qcc.graph;

import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 * The value of the current thread object.  In methods, the current thread is translated into a constant value;
 * in functions, the current thread can be changed or cleared and is stored in a variable.
 */
public final class CurrentThreadRead extends AbstractValue implements OrderedNode {
    private final Node dependency;
    private final ReferenceType type;

    CurrentThreadRead(Node callSite, ExecutableElement element, int line, int bci, Node dependency, ReferenceType type) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.type = type;
    }

    @Override
    int calcHashCode() {
        return dependency.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrentThreadRead && equals((CurrentThreadRead) other);
    }

    public boolean equals(final CurrentThreadRead other) {
        return this == other || other != null && dependency.equals(other.dependency);
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    @Override
    public ValueType getType() {
        return type;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }
}
