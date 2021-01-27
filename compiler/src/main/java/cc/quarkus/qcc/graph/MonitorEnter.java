package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public class MonitorEnter extends AbstractNode implements Action, InstanceOperation {
    private final Node dependency;
    private final Value instance;

    MonitorEnter(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value instance) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.instance = instance;
    }

    public Value getInstance() {
        return instance;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(MonitorEnter.class, dependency, instance);
    }

    public boolean equals(final Object other) {
        return other instanceof MonitorEnter && equals((MonitorEnter) other);
    }

    public boolean equals(final MonitorEnter other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance);
    }
}
