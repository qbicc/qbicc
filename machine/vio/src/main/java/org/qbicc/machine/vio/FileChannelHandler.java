package org.qbicc.machine.vio;

import java.io.IOException;
import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import io.smallrye.common.function.ExceptionRunnable;

final class FileChannelHandler implements IoHandler, ReadableIoHandler, WritableIoHandler, SeekableIoHandler, StatableIoHandler {
    private static final VarHandle closedHandle = ConstantBootstraps.fieldVarHandle(MethodHandles.lookup(), "closed", VarHandle.class, FileChannelHandler.class, boolean.class);
    private final FileChannel channel;
    private final boolean append;
    private final ByteBuffer singleByte = ByteBuffer.allocateDirect(1);
    private final ExceptionRunnable<IOException> closeAction;
    private volatile boolean closed;

    FileChannelHandler(final FileChannel channel, boolean append, ExceptionRunnable<IOException> closeAction) {
        this.channel = channel;
        this.append = append;
        this.closeAction = closeAction;
    }

    @Override
    public void close() throws IOException {
        if (closedHandle.compareAndSet(this, false, true)) {
            try {
                channel.close();
            } catch (Throwable t) {
                try {
                    closeAction.run();
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
            closeAction.run();
        }
    }

    @Override
    public int read(ByteBuffer buf) throws IOException {
        return channel.read(buf);
    }

    @Override
    public int readSingle() throws IOException {
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        int cnt = channel.read(singleByte);
        return cnt == -1 ? -1 : singleByte.get(0);
    }

    @Override
    public long available() throws IOException {
        long size = channel.size();
        long offset = channel.position();
        return Math.max(0, size - offset);
    }

    @Override
    public int write(ByteBuffer buf) throws IOException {
        return channel.write(buf);
    }

    @Override
    public int append(ByteBuffer buf) throws IOException {
        channel.position(channel.size());
        return channel.write(buf);
    }

    @Override
    public void writeSingle(int value) throws IOException {
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        singleByte.put(0, (byte) value);
        if (channel.write(singleByte) == 0) {
            throw new IOException("Would block");
        }
    }

    @Override
    public void appendSingle(int value) throws IOException {
        channel.position(channel.size());
        ByteBuffer singleByte = this.singleByte;
        singleByte.position(0);
        singleByte.put(0, (byte) value);
        if (channel.write(singleByte) == 0) {
            throw new IOException("Would block");
        }
    }

    @Override
    public boolean isAppend() {
        return append;
    }

    @Override
    public long seekRelative(long offs) throws IOException {
        long oldPosition = channel.position();
        if (offs != 0) {
            channel.position(oldPosition + offs);
        }
        return oldPosition;
    }

    @Override
    public long seekAbsolute(long offs) throws IOException {
        long oldPosition = channel.position();
        if (offs != oldPosition) {
            channel.position(offs);
        }
        return oldPosition;
    }

    @Override
    public long getSize() throws IOException {
        return channel.size();
    }
}
