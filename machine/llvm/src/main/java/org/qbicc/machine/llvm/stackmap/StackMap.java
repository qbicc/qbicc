package org.qbicc.machine.llvm.stackmap;

import java.nio.ByteBuffer;

/**
 * Entry point for parsing stack maps.
 */
public final class StackMap {
    private StackMap() {}

    public static void parse(ByteBuffer stackMapBuffer, StackMapVisitor visitor) {
        ByteBuffer buf = stackMapBuffer.duplicate().order(stackMapBuffer.order());
        int version = buf.get() & 0xff;
        if (version == 3) {
            buf.get();
            buf.getShort();
            long fnCnt = buf.getInt() & 0xFFFF_FFFFL;
            long constCnt = buf.getInt() & 0xFFFF_FFFFL;
            long recCnt = buf.getInt() & 0xFFFF_FFFFL;
            ByteBuffer stackSizesBuf = buf.duplicate().order(buf.order());
            buf.position((int) (buf.position() + 24 * fnCnt));
            int constEnd = (int) (buf.position() + constCnt * 8);
            ByteBuffer constBuf = buf.duplicate().limit(constEnd).slice().order(buf.order());
            buf.position(constEnd);
            // buf now points to the stack map record sequence
            visitor.start(version, fnCnt, recCnt);
            for (long i = 0; i < fnCnt; i ++) {
                long addr = stackSizesBuf.getLong();
                long stackSize = stackSizesBuf.getLong();
                long fnRecCnt = stackSizesBuf.getLong();
                visitor.startFunction(i, addr, stackSize, fnRecCnt);
                // records
                for (long j = 0; j < fnRecCnt; j ++) {
                    long ppId = buf.getLong();
                    long offs = buf.getInt() & 0xFFFF_FFFFL;
                    buf.getShort();
                    int locCnt = buf.getShort() & 0xFFFF;
                    // peek way ahead
                    int jump = buf.position() + locCnt * 12;
                    if ((jump & 0x7) != 0) {
                        // align to 8 bytes
                        jump += 4;
                    }
                    jump += 2; // unconditional padding
                    int liveOutCnt = buf.getShort(jump) & 0xFFFF;
                    visitor.startRecord(j, ppId, offs, locCnt, liveOutCnt);
                    // locations
                    for (int k = 0; k < locCnt; k ++) {
                        int encType = buf.get() & 0xff;
                        LocationType type = LocationType.forEncoding(encType);
                        buf.get();
                        int locSize = buf.getShort() & 0xFFFF;
                        int regNum = buf.getShort() & 0xFFFF;
                        buf.getShort();
                        long data = encType == 0x5 ? constBuf.getLong(buf.getInt() * 8) : buf.getInt();
                        visitor.location(k, type, locSize, regNum, data);
                    }
                    // padding
                    if ((buf.position() & 0x7) != 0) {
                        buf.getInt();
                    }
                    buf.getShort();
                    int liveOutCnt2 = buf.getShort() & 0xFFFF;
                    assert liveOutCnt2 == liveOutCnt;
                    // live outs
                    for (int k = 0; k < liveOutCnt; k ++) {
                        int regNum = buf.getShort() & 0xFFFF;
                        buf.get();
                        int size = buf.get() & 0xFF;
                        visitor.liveOut(k, regNum, size);
                    }
                    // padding
                    if ((buf.position() & 0x7) != 0) {
                        buf.getInt();
                    }
                    visitor.endRecord(j);
                }
                visitor.endFunction(i);
            }
            visitor.end();
        } else {
            throw new IllegalArgumentException("Unrecognized stack map version " + version);
        }
    }
}
