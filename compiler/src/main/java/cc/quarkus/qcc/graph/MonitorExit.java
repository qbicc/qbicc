package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public class MonitorExit extends AbstractNode implements Action, InstanceOperation, OrderedNode {
    private final Node dependency;
    private final Value instance;

    MonitorExit(final Node callSite, final ExecutableElement element, final int line, final int bci, final Node dependency, final Value instance) {
        super(callSite, element, line, bci);
        this.dependency = dependency;
        this.instance = instance;
    }

    public Value getInstance() {
        return instance;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(MonitorExit.class, dependency, instance);
    }

    public boolean equals(final Object other) {
        return other instanceof MonitorExit && equals((MonitorExit) other);
    }

    public boolean equals(final MonitorExit other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance);
    }
}
