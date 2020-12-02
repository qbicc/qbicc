package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.type.descriptor.ParameterizedExecutableDescriptor;

/**
 *
 */
public final class NoSuchMethodErrorNode extends AbstractNode implements Error {
    private final Node dependency;
    private final TypeIdLiteral owner;
    private final ParameterizedExecutableDescriptor desc;
    private final String name;

    NoSuchMethodErrorNode(final int line, final int bci, final Node dependency, final TypeIdLiteral owner, final ParameterizedExecutableDescriptor desc, final String name) {
        super(line, bci);
        this.dependency = dependency;
        this.owner = owner;
        this.desc = desc;
        this.name = name;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public TypeIdLiteral getOwner() {
        return owner;
    }

    public ParameterizedExecutableDescriptor getDescriptor() {
        return desc;
    }

    public String getName() {
        return name;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, owner, desc, name);
    }

    public boolean equals(final Object other) {
        return other instanceof NoSuchMethodErrorNode && equals((NoSuchMethodErrorNode) other);
    }

    public boolean equals(final NoSuchMethodErrorNode other) {
        return this == other || other != null
            && dependency.equals(other.dependency)
            && owner.equals(other.owner)
            && desc.equals(other.desc)
            && name.equals(other.name);
    }
}
