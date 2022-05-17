package org.qbicc.machine.vio;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class SystemTests {
    private static final Path resourcesPath = Path.of(System.getProperty("qbicc.test.resourcesPath"));

    private static VIOSystem system;

    @BeforeAll
    public static void setUp() {
        system = new VIOSystem(64);
    }

    @Test
    public void testSimpleReadArray() throws IOException {
        doReadTest(ByteBuffer.allocate(256));
    }

    @Test
    public void testSimpleReadDirect() throws IOException {
        doReadTest(ByteBuffer.allocateDirect(256));
    }

    private void doReadTest(final ByteBuffer buf) throws IOException {
        Path testInputPath = resourcesPath.resolve("test-input.txt");
        String testStr = Files.readString(testInputPath);
        byte[] strBytes = testStr.getBytes(StandardCharsets.UTF_8);
        int fd = system.openRealFile(testInputPath, Set.of(StandardOpenOption.READ), Set.of());
        try {
            int res = system.read(fd, buf);
            assertEquals(strBytes.length, res);
            buf.flip();
            assertEquals(buf, ByteBuffer.wrap(strBytes));
        } catch (Throwable t) {
            safeClose(fd, t);
            throw t;
        }
        system.close(fd);
    }

    private static void safeClose(int fd, Throwable t) {
        try {
            system.close(fd);
        } catch (Throwable t2) {
            if (t != null) {
                t.addSuppressed(t);
            }
        }
    }
}
