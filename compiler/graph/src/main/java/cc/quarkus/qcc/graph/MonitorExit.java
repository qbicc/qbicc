package cc.quarkus.qcc.graph;

/**
 *
 */
public class MonitorExit extends AbstractNode implements Action, InstanceOperation {
    private final Node dependency;
    private final Value instance;

    MonitorExit(final int line, final int bci, final Node dependency, final Value instance) {
        super(line, bci);
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
}
