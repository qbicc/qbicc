package org.qbicc.machine.llvm;

import java.nio.ByteOrder;

/**
 * The data layout configuration for the target.
 */
public interface DataLayout {
    DataLayout byteOrder(ByteOrder byteOrder);

    DataLayout stackAlign(int stackAlign);

    DataLayout pointerSize(int pointerSize);

    DataLayout pointerAlign(int pointerAlign);

    DataLayout refSize(int refSize);

    DataLayout refAlign(int refAlign);

    DataLayout int8Align(int int8Align);

    DataLayout int16Align(int int16Align);

    DataLayout int32Align(int int32Align);

    DataLayout int64Align(int int64Align);

    DataLayout int128Align(int int128Align);

    DataLayout float32Align(int float32Align);

    DataLayout float64Align(int float64Align);

    DataLayout aggregateAlign(int aggregateAlign);

    DataLayout mangling(SymbolMangling mangling);

    DataLayout nativeWidths(int... nativeWidths);
}
