package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface CompoundCType extends ObjectCType {
    default boolean isCompound() {
        return true;
    }

    default CompoundCType asCompound() {
        return this;
    }

    int getMemberCount();

    Member getMember(int index) throws IndexOutOfBoundsException;

    interface Member {
        String getName();

        ObjectCType getType();
    }
}
