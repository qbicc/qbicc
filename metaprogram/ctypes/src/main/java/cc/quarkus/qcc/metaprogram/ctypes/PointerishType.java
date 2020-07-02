package cc.quarkus.qcc.metaprogram.ctypes;

import cc.quarkus.qcc.graph.NativeObjectType;
import cc.quarkus.qcc.graph.WordType;

/**
 *
 */
public interface PointerishType extends WordType {
    boolean isRestrict();

    NativeObjectType getEnclosedType();
}
