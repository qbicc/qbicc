package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.nio.ByteOrder;

import org.qbicc.machine.llvm.DataLayout;
import org.qbicc.machine.llvm.SymbolMangling;

final class DataLayoutImpl extends AbstractEmittable implements DataLayout {
    private ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
    private int stackAlign = 0;
    private int pointerSize = -1;
    private int pointerAlign = -1;
    private int refSize = -1;
    private int refAlign = -1;
    private int int8Align = 8;
    private int int16Align = 16;
    private int int32Align = 32;
    private int int64Align = 64;
    private int int128Align = 128;
    private int float32Align = 32;
    private int float64Align = 64;
    private int aggregateAlign = 0;
    private SymbolMangling mangling;
    private int[] nativeWidths;

    DataLayoutImpl() {
    }

    @Override
    public DataLayout byteOrder(ByteOrder byteOrder) {
        this.byteOrder = byteOrder;
        return this;
    }

    @Override
    public DataLayout stackAlign(int stackAlign) {
        this.stackAlign = stackAlign;
        return this;
    }

    @Override
    public DataLayout pointerSize(int pointerSize) {
        this.pointerSize = pointerSize;
        return this;
    }

    @Override
    public DataLayout pointerAlign(int pointerAlign) {
        this.pointerAlign = pointerAlign;
        return this;
    }

    @Override
    public DataLayout refSize(int refSize) {
        this.refSize = refSize;
        return this;
    }

    @Override
    public DataLayout refAlign(int refAlign) {
        this.refAlign = refAlign;
        return this;
    }

    @Override
    public DataLayout int8Align(int int8Align) {
        this.int8Align = int8Align;
        return this;
    }

    @Override
    public DataLayout int16Align(int int16Align) {
        this.int16Align = int16Align;
        return this;
    }

    @Override
    public DataLayout int32Align(int int32Align) {
        this.int32Align = int32Align;
        return this;
    }

    @Override
    public DataLayout int64Align(int int64Align) {
        this.int64Align = int64Align;
        return this;
    }

    @Override
    public DataLayout int128Align(int int128Align) {
        this.int128Align = int128Align;
        return this;
    }

    @Override
    public DataLayout float32Align(int float32Align) {
        this.float32Align = float32Align;
        return this;
    }

    @Override
    public DataLayout float64Align(int float64Align) {
        this.float64Align = float64Align;
        return this;
    }

    @Override
    public DataLayout aggregateAlign(int aggregateAlign) {
        this.aggregateAlign = aggregateAlign;
        return this;
    }

    @Override
    public DataLayout mangling(SymbolMangling mangling) {
        this.mangling = mangling;
        return this;
    }

    @Override
    public DataLayout nativeWidths(int... nativeWidths) {
        this.nativeWidths = nativeWidths;
        return this;
    }

    @Override
    public Appendable appendTo(Appendable target) throws IOException {
        target.append("target datalayout = ");
        StringBuilder b = new StringBuilder(40);
        if (byteOrder == ByteOrder.BIG_ENDIAN) {
            b.append("E");
        } else {
            b.append("e");
        }
        if (stackAlign != 0) {
            b.append("-S").append(stackAlign);
        }
        if (pointerSize != -1 && pointerAlign != -1) {
            b.append("-p0:").append(pointerSize).append(':').append(pointerAlign);
        }
        if (refSize != -1 && refAlign != -1) {
            b.append("-p1:").append(refSize).append(':').append(refAlign);
        }
        if (int8Align != 8) {
            b.append("-i8:").append(int8Align);
        }
        if (int16Align != 16) {
            b.append("-i16:").append(int16Align);
        }
        if (int32Align != 32) {
            b.append("-i32:").append(int32Align);
        }
        if (int64Align != 64) {
            b.append("-i64:").append(int64Align);
        }
        if (int128Align != 128) {
            b.append("-i128:").append(int128Align);
        }
        if (float32Align != 32) {
            b.append("-f32:").append(float32Align);
        }
        if (float64Align != 64) {
            b.append("-f64:").append(float64Align);
        }
        if (aggregateAlign != 0) {
            b.append("-a:").append(aggregateAlign);
        }
        if (mangling != null) {
            b.append("-m:").append(mangling.getDataLayoutCharacter());
        }
        if (nativeWidths != null && nativeWidths.length > 0) {
            b.append('n').append(nativeWidths[0]);
            for (int i = 1; i < nativeWidths.length; i ++) {
                b.append(':').append(nativeWidths[i]);
            }
        }
        appendEscapedString(target, b.toString());
        return target;
    }
}
