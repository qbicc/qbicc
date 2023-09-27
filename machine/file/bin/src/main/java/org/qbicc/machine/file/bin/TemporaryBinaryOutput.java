package org.qbicc.machine.file.bin;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.smallrye.common.function.ExceptionConsumer;

/**
 *
 */
class TemporaryBinaryOutput extends ChannelBinaryOutput {
    private FileChannel fc;
    private Path path;
    private final ExceptionConsumer<BinaryInput, IOException> onClose;

    TemporaryBinaryOutput(ByteOrder order, ExceptionConsumer<BinaryInput, IOException> onClose) {
        super(order);
        this.onClose = onClose;
    }

    @Override
    FileChannel channel() throws IOException {
        if (fc == null) {
            path = Files.createTempFile("temp-", ".bin");
            try {
                fc = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                try {
                    Files.deleteIfExists(path);
                } catch (Throwable t) {
                    e.addSuppressed(t);
                }
                throw e;
            }
        }
        return fc;
    }

    @Override
    public void close() throws IOException {
        if (fc == null) {
            super.close();
            ByteBuffer buffer = buffer();
            buffer.flip();
            onClose.accept(BinaryInput.create(buffer));
        } else {
            try (FileChannel fc = this.fc) {
                flush();
                super.close();
                fc.position(0);
                try (BufferBinaryInput bi = BinaryInput.open(fc, order())) {
                    onClose.accept(bi);
                }
            } catch (Throwable t) {
                try {
                    Files.deleteIfExists(path);
                } catch (Throwable t2) {
                    t.addSuppressed(t2);
                }
                throw t;
            }
            Files.deleteIfExists(path);
        }
    }
}
