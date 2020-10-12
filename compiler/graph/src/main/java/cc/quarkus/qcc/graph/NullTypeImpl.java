package cc.quarkus.qcc.graph;

final class NullTypeImpl extends AbstractType implements NullType {
    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
    }

    public Object boxValue(final ConstantValue value) {
        // todo: this probably isn't going to work long-term due to Maps being picky about null values...
        return null;
    }
}
