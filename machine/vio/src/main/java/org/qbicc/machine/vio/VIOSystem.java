package org.qbicc.machine.vio;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.Pipe;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import io.smallrye.common.function.ExceptionBiConsumer;
import io.smallrye.common.function.ExceptionBiFunction;
import io.smallrye.common.function.ExceptionBiPredicate;
import io.smallrye.common.function.ExceptionConsumer;
import io.smallrye.common.function.ExceptionFunction;
import io.smallrye.common.function.ExceptionObjIntConsumer;
import io.smallrye.common.function.ExceptionObjLongConsumer;
import io.smallrye.common.function.ExceptionPredicate;
import io.smallrye.common.function.ExceptionRunnable;
import io.smallrye.common.function.ExceptionSupplier;
import io.smallrye.common.function.ExceptionToLongBiFunction;
import io.smallrye.common.function.ExceptionToLongFunction;

/**
 * A UNIX-like virtual I/O system.
 */
public final class VIOSystem {

    private final VIOFileDescriptor[] fileDescArray;

    /**
     * Construct a new instance.
     *
     * @param maxFiles the maximum number of open files to allow
     */
    public VIOSystem(int maxFiles) {
        int len = Math.max(16, maxFiles);
        VIOFileDescriptor[] array = new VIOFileDescriptor[len];
        for (int i = 0; i < len ; i ++) {
            array[i] = new VIOFileDescriptor(i);
        }
        fileDescArray = array;
    }

    /**
     * Open a file descriptor which will use the handler produced by the given factory.
     *
     * @param factory the I/O handler factory (must not be {@code null} or produce {@code null})
     * @return the new file descriptor
     * @throws IOException if a file descriptor cannot be opened
     * @throws IllegalStateException if the factory produces a {@code null} handler
     */
    public int open(ExceptionSupplier<IoHandler, IOException> factory) throws IOException {
        return allocateFd(newDesc -> {
            // create the file
            VIOFile file = new VIOFile();
            synchronized (file) {
                // attempt to open it
                file.open(factory);
                newDesc.setFile(file);
            }
        }).getFdNum();
    }

    /**
     * Replace a file descriptor in the manner of {@link #dup2}, but which will use the handler produced by the given factory.
     *
     * @param fd the file descriptor to replace
     * @param factory the I/O handler factory (must not be {@code null} or produce {@code null})
     * @throws IOException if the file cannot be opened
     * @throws IllegalStateException if the factory produces a {@code null} handler
     */
    public void replace(int fd, ExceptionSupplier<IoHandler, IOException> factory) throws IOException {
        rangeCheck(fd);
        VIOFile file = new VIOFile();
        // attempt to open it
        file.open(factory);
        VIOFile oldFile = fileDescArray[fd].replaceFile(file);
        if (oldFile != null) {
            try {
                oldFile.releaseOne();
            } catch (IOException ignored) {}
        }
    }

    public int openInputStream(ExceptionSupplier<InputStream, IOException> factory) throws IOException {
        return open(() -> new InputStreamHandler(factory.get()));
    }

    public void openInputStream(int fd, ExceptionSupplier<InputStream, IOException> factory) throws IOException {
        replace(fd, () -> new InputStreamHandler(factory.get()));
    }

    public int openOutputStream(ExceptionSupplier<OutputStream, IOException> factory) throws IOException {
        return open(() -> new OutputStreamHandler(factory.get()));
    }

    public void openOutputStream(int fd, ExceptionSupplier<OutputStream, IOException> factory) throws IOException {
        replace(fd, () -> new OutputStreamHandler(factory.get()));
    }

    public int openPipeSource(ExceptionSupplier<Pipe.SourceChannel, IOException> factory) throws IOException {
        return open(() -> new PipeSourceHandler(factory.get()));
    }

    public void openPipeSource(int fd, ExceptionSupplier<Pipe.SourceChannel, IOException> factory) throws IOException {
        replace(fd, () -> new PipeSourceHandler(factory.get()));
    }

    public int openPipeSink(ExceptionSupplier<Pipe.SinkChannel, IOException> factory) throws IOException {
        return open(() -> new PipeSinkHandler(factory.get()));
    }

    public void openPipeSink(int fd, ExceptionSupplier<Pipe.SinkChannel, IOException> factory) throws IOException {
        replace(fd, () -> new PipeSinkHandler(factory.get()));
    }

    /**
     * Open a pipe.
     *
     * @return the pipe ends as a 2-element array of {@code { readFd, writeFd }}
     * @throws IOException if an error occurs when creating the pipe
     */
    public int[] pipe() throws IOException {
        int readFd, writeFd;
        Pipe pipe = Pipe.open();
        try {
            readFd = openPipeSource(pipe::source);
            try {
                writeFd = openPipeSink(pipe::sink);
            } catch (Throwable t) {
                safeClose(readFd, t);
                throw t;
            }
        } catch (Throwable t) {
            safeClose(pipe.source(), t);
            safeClose(pipe.sink(), t);
            throw t;
        }
        return new int[] { readFd, writeFd };
    }

    /**
     * Open a "socket pair".
     *
     * @return the socket pair as a 2-element array of file descriptors
     * @throws IOException if an error occurs when creating the socket pair
     */
    public int[] socketPair() throws IOException {
        int leftFd, rightFd;
        Pipe a = Pipe.open();
        try {
            Pipe b = Pipe.open();
            try {
                leftFd = open(() -> new SocketPairHandler(a.source(), b.sink()));
                try {
                    rightFd = open(() -> new SocketPairHandler(b.source(), a.sink()));
                } catch (Throwable t) {
                    safeClose(leftFd, t);
                    throw t;
                }
            } catch (Throwable t) {
                safeClose(b.source(), t);
                safeClose(b.sink(), t);
                throw t;
            }
        } catch (Throwable t) {
            safeClose(a.source(), t);
            safeClose(a.sink(), t);
            throw t;
        }
        return new int[] { leftFd, rightFd };
    }

    /**
     * Open a file descriptor for a real file on the host file system.
     *
     * @param path the host path (must not be {@code null})
     * @param options the open options (must not be {@code null}, may be empty)
     * @return the file descriptor
     * @throws IOException if the open fails
     */
    public int openRealFile(Path path, Set<? extends OpenOption> options, Set<? extends FileAttribute<?>> attrs) throws IOException {
        return openRealFile(path, options, attrs, () -> {});
    }

    public int openRealFile(Path path, Set<? extends OpenOption> options, Set<? extends FileAttribute<?>> attrs, ExceptionRunnable<IOException> closeAction) throws IOException {
        boolean append = options.contains(StandardOpenOption.APPEND);
        return open(() -> new FileChannelHandler(FileChannel.open(path, options, attrs.toArray(FileAttribute[]::new)), append, closeAction));
    }

    public void openRealFile(int fd, Path path, Set<? extends OpenOption> options, Set<? extends FileAttribute<?>> attrs) throws IOException {
        openRealFile(fd, path, options, attrs, () -> {});
    }

    public void openRealFile(int fd, Path path, Set<? extends OpenOption> options, Set<? extends FileAttribute<?>> attrs, ExceptionRunnable<IOException> closeAction) throws IOException {
        boolean append = options.contains(StandardOpenOption.APPEND);
        replace(fd, () -> new FileChannelHandler(FileChannel.open(path, options, attrs.toArray(FileAttribute[]::new)), append, closeAction));
    }

    /**
     * Open a file descriptor for reading a zip file entry.
     *
     * @param zf the open zip file (must not be {@code null})
     * @param ze the zip entry (must not be {@code null})
     * @return the file descriptor
     * @throws IOException if the open fails
     */
    public int openZipFileEntryForInput(ZipFile zf, ZipEntry ze) throws IOException {
        return open(() -> new InputStreamHandler(zf.getInputStream(ze)));
    }

    public void openZipFileEntryForInput(int fd, ZipFile zf, ZipEntry ze) throws IOException {
        replace(fd, () -> new InputStreamHandler(zf.getInputStream(ze)));
    }

    /**
     * Duplicate the given file descriptor.
     *
     * @param origFd the original file descriptor number
     * @return the duplicate file descriptor number
     * @throws IOException if duplication failed
     */
    public int dup(int origFd) throws IOException {
        rangeCheck(origFd);
        return allocateFd(newDesc -> {
            VIOFile file = fileDescArray[origFd].getFile();
            if (file == null) {
                throw BadDescriptorException.fileNotOpen();
            }
            file.acquireOne();
            newDesc.setFile(file);
        }).getFdNum();
    }

    /**
     * Duplicate the given file descriptor into the specified descriptor slot.
     *
     * @param origFd the original file descriptor number
     * @param newFd the new file descriptor number
     * @throws IOException if duplication failed
     */
    public void dup2(int origFd, int newFd) throws IOException {
        rangeCheck(newFd);
        if (origFd == newFd) {
            throw new BadDescriptorException("origFd must not equal newFd");
        }
        VIOFile file = findFile(origFd);
        file.acquireOne();
        VIOFile oldFile = fileDescArray[newFd].replaceFile(file);
        if (oldFile != null) {
            try {
                oldFile.releaseOne();
            } catch (IOException ignored) {}
        }
    }

    /**
     * Close the given file descriptor.
     *
     * @param fd the file descriptor to close
     * @throws IOException if the backing file close failed
     */
    public void close(int fd) throws IOException {
        findFile(fd).releaseOne();
    }

    // duplex ops

    public void shutdownInput(int fd) throws IOException {
        run(fd, DuplexIoHandler.class, DuplexIoHandler::shutdownInput);
    }

    public void shutdownOutput(int fd) throws IOException {
        run(fd, DuplexIoHandler.class, DuplexIoHandler::shutdownOutput);
    }

    // read ops

    public int read(int fd, ByteBuffer buffer) throws IOException {
        return (int) call(fd, ReadableIoHandler.class, buffer, ReadableIoHandler::read);
    }

    public int readSingle(final int fd) throws IOException {
        return (int) call(fd, ReadableIoHandler.class, ReadableIoHandler::readSingle);
    }

    public long available(final int fd) throws IOException {
        return call(fd, ReadableIoHandler.class, ReadableIoHandler::available);
    }

    // write ops

    public int write(int fd, ByteBuffer buffer) throws IOException {
        return (int) call(fd, WritableIoHandler.class, buffer, WritableIoHandler::write);
    }

    public void writeSingle(final int fd, final int val) throws IOException {
        run(fd, WritableIoHandler.class, val, WritableIoHandler::writeSingle);
    }

    public boolean isAppend(int fd) throws IOException {
        return test(fd, WritableIoHandler.class, WritableIoHandler::isAppend);
    }

    // file ops

    public long getFileSize(final int fd) throws IOException {
        return call(fd, StatableIoHandler.class, StatableIoHandler::getSize);
    }

    public long seekAbsolute(final int fd, final long offs) throws IOException {
        return call(fd, SeekableIoHandler.class, offs, SeekableIoHandler::seekAbsolute);
    }

    public long seekRelative(final int fd, final long offs) throws IOException {
        return call(fd, SeekableIoHandler.class, offs, SeekableIoHandler::seekRelative);
    }

    // generic ops

    public <H extends IoHandler> void run(int fd, Class<H> handlerType, ExceptionConsumer<H, IOException> action) throws IOException {
        findFile(fd).run(handlerType, action);
    }

    public <H extends IoHandler, T> void run(int fd, Class<H> handlerType, T argument, ExceptionBiConsumer<H, T, IOException> action) throws IOException {
        findFile(fd).run(handlerType, argument, action);
    }

    public <H extends IoHandler> void run(int fd, Class<H> handlerType, long argument, ExceptionObjLongConsumer<H, IOException> action) throws IOException {
        findFile(fd).run(handlerType, argument, action);
    }

    public <H extends IoHandler> void run(int fd, Class<H> handlerType, int argument, ExceptionObjIntConsumer<H, IOException> action) throws IOException {
        findFile(fd).run(handlerType, argument, action);
    }

    public <H extends IoHandler, R> R call(int fd, Class<H> handlerType, ExceptionFunction<H, R, IOException> action) throws IOException {
        return findFile(fd).call(handlerType, action);
    }

    public <H extends IoHandler, T, R> R call(int fd, Class<H> handlerType, T argument, ExceptionBiFunction<H, T, R, IOException> action) throws IOException {
        return findFile(fd).call(handlerType, argument, action);
    }

    public <H extends IoHandler> long call(int fd, Class<H> handlerType, ExceptionToLongFunction<H, IOException> action) throws IOException {
        return findFile(fd).call(handlerType, action);
    }

    public <H extends IoHandler> long call(int fd, Class<H> handlerType, long argument, ExceptionObjLongToLongFunction<H, IOException> action) throws IOException {
        return findFile(fd).call(handlerType, argument, action);
    }

    public <H extends IoHandler, T> long call(int fd, Class<H> handlerType, T argument, ExceptionToLongBiFunction<H, T, IOException> action) throws IOException {
        return findFile(fd).call(handlerType, argument, action);
    }

    public <H extends IoHandler> boolean test(int fd, Class<H> handlerType, ExceptionPredicate<H, IOException> action) throws IOException {
        return findFile(fd).test(handlerType, action);
    }

    public <H extends IoHandler, T> boolean test(int fd, Class<H> handlerType, T argument, ExceptionBiPredicate<H, T, IOException> action) throws IOException {
        return findFile(fd).test(handlerType, argument, action);
    }

    // -------
    // private
    // -------

    private VIOFile findFile(final int fd) throws BadDescriptorException {
        rangeCheck(fd);
        VIOFileDescriptor desc = fileDescArray[fd];
        VIOFile file = desc.getFile();
        if (file == null) {
            throw BadDescriptorException.fileNotOpen();
        }
        return file;
    }

    private void rangeCheck(final int origFd) throws BadDescriptorException {
        if (origFd < 0 || origFd >= fileDescArray.length) {
            throw new BadDescriptorException("File descriptor out of range");
        }
    }

    private VIOFileDescriptor allocateFd(ExceptionConsumer<VIOFileDescriptor, IOException> action) throws IOException {
        // for now, linear search; later: sorted free list
        for (VIOFileDescriptor desc : fileDescArray) {
            if (desc.tryOpen(action)) {
                return desc;
            }
        }
        throw new TooManyOpenFilesException();
    }

    private void safeClose(final Closeable closeable, final Throwable t) {
        try {
            closeable.close();
        } catch (Throwable t2) {
            t.addSuppressed(t2);
        }
    }

    private void safeClose(final int fd, final Throwable t) {
        try {
            close(fd);
        } catch (Throwable t2) {
            t.addSuppressed(t2);
        }
    }
}
