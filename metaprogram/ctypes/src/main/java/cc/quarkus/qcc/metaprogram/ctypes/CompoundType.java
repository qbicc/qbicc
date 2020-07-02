package cc.quarkus.qcc.metaprogram.ctypes;

import cc.quarkus.qcc.graph.NativeObjectType;
import cc.quarkus.qcc.graph.Type;

/**
 *
 */
public interface CompoundType extends NativeObjectType {
    int getMemberCount();

    Member getMember(int index) throws IndexOutOfBoundsException;

    interface Member {
        int getAlignment();

        int getOffset();

        String getName();

        Type getType();
    }
}
