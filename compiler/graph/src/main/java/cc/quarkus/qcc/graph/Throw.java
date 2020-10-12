package cc.quarkus.qcc.graph;

/**
 * An instruction which throws an exception <em>to the caller</em>.  To throw an exception to a catch block,
 * use {@link Goto}.
 */
public final class Throw extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value thrownValue;

    private Throw(final Node dependency, final Value thrownValue) {
        this.dependency = dependency;
        this.thrownValue = thrownValue;
    }

    public Value getThrownValue() {
        return thrownValue;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? thrownValue : Util.throwIndexOutOfBounds(index);
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static void create(GraphFactory.Context ctxt, Value thrownValue) {
        Throw throw_ = new Throw(ctxt.getDependency(), thrownValue);
        ctxt.getCurrentBlock().setTarget(new BasicBlock(throw_));
        ctxt.setCurrentBlock(null);
    }
}
