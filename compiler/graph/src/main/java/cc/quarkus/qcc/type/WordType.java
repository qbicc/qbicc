package cc.quarkus.qcc.type;

/**
 * Values of this type are generally represented numerically by one or more machine words, and have a minimum and actual
 * size.
 */
public abstract class WordType extends ScalarType {
    WordType(final TypeSystem typeSystem, final int hashCode, final boolean const_) {
        super(typeSystem, hashCode, const_);
    }

    public WordType asConst() {
        return (WordType) super.asConst();
    }

    /**
     * Get the declared number of bits for this type.  The actual size may comprise more bits than are declared.
     *
     * @return the minimum size (in bits) of this type
     */
    public abstract int getMinBits();
}
