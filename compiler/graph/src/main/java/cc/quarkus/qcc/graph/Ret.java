package cc.quarkus.qcc.graph;

/**
 *
 */
public final class Ret extends AbstractNode implements Terminator {
    private final Node dependency;
    private final Value returnAddressValue;

    private Ret(final Node dependency, final Value returnAddressValue) {
        this.dependency = dependency;
        this.returnAddressValue = returnAddressValue;
    }

    public Value getReturnAddressValue() {
        return returnAddressValue;
    }

    public int getValueDependencyCount() {
        return 1;
    }

    public Value getValueDependency(final int index) throws IndexOutOfBoundsException {
        return index == 0 ? returnAddressValue : Util.throwIndexOutOfBounds(index);
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

    static void create(final GraphFactory.Context ctxt, final Value address) {
        Ret ret = new Ret(ctxt.getDependency(), address);
        ctxt.getCurrentBlock().setTarget(new BasicBlock(ret));
        ctxt.setCurrentBlock(null);
    }
}
