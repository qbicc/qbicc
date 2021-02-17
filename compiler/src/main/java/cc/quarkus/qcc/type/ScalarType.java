package cc.quarkus.qcc.type;

/**
 *
 */
public abstract class ScalarType extends ValueType {
    ScalarType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }
}
