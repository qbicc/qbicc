package cc.quarkus.qcc.type;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Values of this type are generally represented numerically by one or more machine words, and have a minimum and actual
 * size.
 */
public abstract class WordType extends ScalarType {
    private static final VarHandle primitiveArrayObjectTypeHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "primitiveArrayObjectType", VarHandle.class, WordType.class, PrimitiveArrayObjectType.class);

    @SuppressWarnings("unused")
    private volatile PrimitiveArrayObjectType primitiveArrayObjectType;

    WordType(final TypeSystem typeSystem, final int hashCode) {
        super(typeSystem, hashCode);
    }

    public PrimitiveArrayObjectType getPrimitiveArrayObjectType() {
        PrimitiveArrayObjectType primitiveArrayObjectType = this.primitiveArrayObjectType;
        if (primitiveArrayObjectType != null) {
            return primitiveArrayObjectType;
        }
        PrimitiveArrayObjectType newPrimitiveArrayObjectType = typeSystem.createPrimitiveArrayObject(this);
        while (! primitiveArrayObjectTypeHandle.compareAndSet(this, null, newPrimitiveArrayObjectType)) {
            primitiveArrayObjectType = this.primitiveArrayObjectType;
            if (primitiveArrayObjectType != null) {
                return primitiveArrayObjectType;
            }
        }
        return newPrimitiveArrayObjectType;
    }

    /**
     * Get the declared number of bits for this type.  The actual size may comprise more bits than are declared.
     *
     * @return the minimum size (in bits) of this type
     */
    public abstract int getMinBits();
}
