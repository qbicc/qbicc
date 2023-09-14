package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

class FileChannelBinaryOutput extends ChannelBinaryOutput {
    private final FileChannel ch;

    FileChannelBinaryOutput(Path path, ByteOrder order) throws IOException {
        super(order);
        ch = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    @Override
    WritableByteChannel channel() {
        return ch;
    }

    @Override
    public void close() throws IOException {
        try {
            flush();
        } catch (IOException e) {
            try {
                ch.close();
            } catch (Throwable t) {
                e.addSuppressed(t);
            }
            throw e;
        }
        ch.close();
    }
}
