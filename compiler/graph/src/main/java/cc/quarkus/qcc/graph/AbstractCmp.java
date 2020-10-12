package cc.quarkus.qcc.graph;

/**
 *
 */
public abstract class AbstractCmp extends AbstractBinaryValue {
    public AbstractCmp(final Value left, final Value right) {
        super(left, right);
    }

    public Type getType() {
        return Type.BOOL;
    }
}
