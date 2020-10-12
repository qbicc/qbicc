package cc.quarkus.qcc.graph;

/**
 *
 */
final class UninitializedTypeImpl extends AbstractType implements UninitializedType {
    private final AbstractClassTypeImpl classType;

    UninitializedTypeImpl(final AbstractClassTypeImpl classType) {
        this.classType = classType;
    }

    public ClassType getClassType() {
        return classType;
    }

    public ArrayClassType getArrayClassType() {
        throw new UnsupportedOperationException();
    }

    public boolean isAssignableFrom(final Type otherType) {
        return false;
    }
}
