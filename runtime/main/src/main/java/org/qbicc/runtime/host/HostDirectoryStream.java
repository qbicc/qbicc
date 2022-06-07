package org.qbicc.runtime.host;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.qbicc.runtime.SerializeBooleanAs;
import org.qbicc.runtime.SerializeIntegralAs;

/**
 * A directory stream on the host.
 */
public final class HostDirectoryStream implements DirectoryStream<Path> {
    private final Path base;
    private final DirectoryStream.Filter<? super Path> filter;

    @SerializeBooleanAs(true)
    private boolean iterated;

    @SerializeIntegralAs(-1)
    private int fd;

    HostDirectoryStream(String pathName, Filter<? super Path> filter) throws IOException {
        this.base = Path.of(pathName);
        this.fd = HostIO.open(pathName, HostIO.O_DIRECTORY | HostIO.O_RDONLY, 0);
        this.filter = filter;
    }

    @Override
    public Iterator<Path> iterator() {
        synchronized (this) {
            if (iterated) {
                throw new IllegalStateException();
            }
            iterated = true;
            return new Itr(base);
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if (fd != -1) {
                HostIO.close(fd);
                fd = -1;
            }
        }
    }

    final class Itr implements Iterator<Path> {
        final Path base;
        Path next;

        Itr(Path base) {
            this.base = base;
        }

        @Override
        public boolean hasNext() {
            synchronized (HostDirectoryStream.this) {
                if (fd == -1) {
                    return false;
                }
                if (next == null) {
                    Path path;
                    boolean ok;
                    String str;
                    Filter<? super Path> filter = HostDirectoryStream.this.filter;
                    do {
                        try {
                            str = HostIO.readDirectoryEntry(fd);
                        } catch (IOException e) {
                            return false;
                        }
                        if (str == null) {
                            return false;
                        }
                        if (str.equals(".") || str.equals("..")) {
                            path = null;
                            ok = false;
                        } else {
                            path = base.resolve(str);
                            try {
                                ok = filter == null || filter.accept(path);
                            } catch (IOException e) {
                                return false;
                            }
                        }
                    } while (! ok);
                    next = path;
                }
            }
            return true;
        }

        @Override
        public Path next() {
            synchronized (HostDirectoryStream.this) {
                if (! hasNext()) {
                    throw new NoSuchElementException();
                }
                Path next = this.next;
                this.next = null;
                return next;
            }
        }
    }
}
