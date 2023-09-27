package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
final class MappedBufferBinaryInput extends BufferBinaryInput {
    private static final VarHandle closedHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "closed", VarHandle.class, MappedBufferBinaryInput.class, boolean.class);

    private final FileChannel ch;
    private final AtomicInteger sharedCount;
    @SuppressWarnings("unused") // closedHandle
    private volatile boolean closed;

    MappedBufferBinaryInput(FileChannel ch, MappedByteBuffer buffer, AtomicInteger sharedCount) {
        super(buffer);
        this.ch = ch;
        this.sharedCount = sharedCount;
    }

    @Override
    MappedByteBuffer buffer() {
        return (MappedByteBuffer) super.buffer();
    }

    @Override
    public BinaryInput fork(ByteOrder order) throws IOException {
        tryFork();
        MappedByteBuffer slice = buffer().slice();
        slice.order(order);
        return new MappedBufferBinaryInput(ch, slice, sharedCount);
    }

    @Override
    public BinaryInput fork(ByteOrder order, long size) throws IOException {
        if (size > buffer().remaining()) {
            return fork();
        }
        tryFork();
        MappedByteBuffer slice = buffer().slice(buffer().position(), (int) size);
        slice.order(order);
        return new MappedBufferBinaryInput(ch, slice, sharedCount);
    }

    private void tryFork() throws IOException {
        int cnt = sharedCount.get();
        do {
            if (cnt == 0) {
                throw new IOException("Use after close");
            }
        } while (! sharedCount.compareAndSet(cnt, cnt + 1));
    }

    @Override
    public void close() throws IOException {
        if (closedHandle.compareAndSet(this, false, true)) {
            if (sharedCount.decrementAndGet() == 0) {
                ch.close();
            }
        }
    }
}
