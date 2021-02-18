package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.definition.element.ExecutableElement;

/**
 *
 */
public final class ClassNotFoundErrorNode extends AbstractTerminator implements Error {
    private final Node dependency;
    private final String name;
    private final BasicBlock terminatedBlock;

    ClassNotFoundErrorNode(final Node callSite, final ExecutableElement element, final int line, final int bci, final BlockEntry blockEntry, final Node dependency, final String name) {
        super(callSite, element, line, bci);
        terminatedBlock = new BasicBlock(blockEntry, this);
        this.dependency = dependency;
        this.name = name;
    }

    public BasicBlock getTerminatedBlock() {
        return terminatedBlock;
    }

    @Override
    public Node getDependency() {
        return dependency;
    }

    public String getName() {
        return name;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    int calcHashCode() {
        return Objects.hash(dependency, name);
    }

    public boolean equals(final Object other) {
        return other instanceof ClassNotFoundErrorNode && equals((ClassNotFoundErrorNode) other);
    }

    public boolean equals(final ClassNotFoundErrorNode other) {
        return this == other || other != null && dependency.equals(other.dependency) && name.equals(other.name);
    }
}
