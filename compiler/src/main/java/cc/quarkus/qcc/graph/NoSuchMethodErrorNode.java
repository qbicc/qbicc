package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ObjectType;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;

/**
 *
 */
public final class NoSuchMethodErrorNode extends AbstractTerminator implements Error {
    private final Node dependency;
    private final ObjectType owner;
    private final MethodDescriptor desc;
    private final String name;
    private final BasicBlock terminatedBlock;

    NoSuchMethodErrorNode(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final ObjectType owner, final MethodDescriptor desc, final String name) {
        super(callSite, element, line, bci);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.owner = owner;
        this.desc = desc;
        this.name = name;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public ObjectType getOwner() {
        return owner;
    }

    public MethodDescriptor getDescriptor() {
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
