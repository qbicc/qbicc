package cc.quarkus.qcc.driver;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

abstract class ClassPathElement implements Closeable {
    ClassPathElement() {}

    abstract String getName();

    abstract Resource getResource(String name) throws IOException;

    static abstract class Resource implements Closeable {
        Resource() {}

        abstract ByteBuffer getBuffer() throws IOException;
    }

    static final Resource NON_EXISTENT = new Resource() {
        ByteBuffer getBuffer() {
            return null;
        }

        public void close() {
        }
    };
}
