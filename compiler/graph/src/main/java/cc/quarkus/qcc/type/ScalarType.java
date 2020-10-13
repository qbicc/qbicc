package cc.quarkus.qcc.type;

/**
 *
 */
public abstract class ScalarType extends ValueType {
    ScalarType(final TypeSystem typeSystem, final int hashCode, final boolean const_) {
        super(typeSystem, hashCode, const_);
    }

    public ScalarType asConst() {
        return (ScalarType) super.asConst();
    }
}
