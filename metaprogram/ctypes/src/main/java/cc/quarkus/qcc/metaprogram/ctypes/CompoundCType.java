package cc.quarkus.qcc.metaprogram.ctypes;

/**
 *
 */
public interface CompoundCType {
    int getMemberCount();

    Member getMember(int index) throws IndexOutOfBoundsException;

    interface Member {
        String getName();

        ObjectCType getType();

        int getOffset();
    }
}
