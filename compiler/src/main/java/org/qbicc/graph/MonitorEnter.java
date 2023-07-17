package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.context.ProgramLocatable;

/**
 *
 */
public class MonitorEnter extends AbstractNode implements Action, InstanceOperation, OrderedNode {
    private final Node dependency;
    private final Value instance;

    MonitorEnter(final ProgramLocatable pl, final Node dependency, final Value instance) {
        super(pl);
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

    public boolean maySafePoint() {
        return true;
    }

    public <T, R> R accept(final ActionVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(MonitorEnter.class, dependency, instance);
    }

    @Override
    String getNodeName() {
        return "MonitorEnter";
    }

    public boolean equals(final Object other) {
        return other instanceof MonitorEnter && equals((MonitorEnter) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        instance.toReferenceString(b);
        b.append(')');
        return b;
    }

    public boolean equals(final MonitorEnter other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && instance.equals(other.instance);
    }
}
