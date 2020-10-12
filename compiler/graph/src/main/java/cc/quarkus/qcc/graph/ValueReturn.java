package cc.quarkus.qcc.graph;

/**
 * A return which returns a non-{@code void} value.
 */
public final class ValueReturn extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value returnValue;

    private ValueReturn(final Node dependency, final Value returnValue) {
        this.dependency = dependency;
        this.returnValue = returnValue;
    }

    public Value getReturnValue() {
        return returnValue;
    }

    public int getBasicDependencyCount() {
        return 1;
    }

    public Node getBasicDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? dependency : Util.throwIndexOutOfBounds(index);
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(int index) throws IndexOutOfBoundsException {
        return index == 0 ? getReturnValue() : Util.throwIndexOutOfBounds(index);
    }

    public <T, R> R accept(final TerminatorVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    static void create(final GraphFactory.Context ctxt, final Value value) {
        ValueReturn return_ = new ValueReturn(ctxt.getDependency(), value);
        ctxt.getCurrentBlock().setTarget(new BasicBlock(return_));
        ctxt.setCurrentBlock(null);
    }
}
