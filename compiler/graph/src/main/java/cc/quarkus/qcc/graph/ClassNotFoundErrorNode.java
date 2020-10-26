package cc.quarkus.qcc.graph;

/**
 *
 */
public final class ClassNotFoundErrorNode extends AbstractNode implements Error {
    private final Node dependency;
    private final String name;

    ClassNotFoundErrorNode(final int line, final int bci, final Node dependency, final String name) {
        super(line, bci);
        this.dependency = dependency;
        this.name = name;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public String getName() {
        return name;
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }
}
