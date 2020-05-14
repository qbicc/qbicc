package cc.quarkus.qcc.machine.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import cc.quarkus.qcc.machine.arch.ObjectType;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CCompiler;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestCTypeProbe {

    static CCompiler compiler;
    static ObjectFileProvider objectFileProvider;

    @BeforeAll
    public static void setUpCompiler() {
        final Iterable<CCompiler> tools = ToolProvider.findAllTools(CCompiler.class, Platform.HOST_PLATFORM, s -> true,
            TestCTypeProbe.class.getClassLoader());
        final Iterator<CCompiler> iterator = tools.iterator();
        assertTrue(iterator.hasNext());
        compiler = iterator.next();
        final ObjectType objectType = Platform.HOST_PLATFORM.getObjectType();
        System.out.println("Local object file type: " + objectType);
        objectFileProvider = ObjectFileProvider.findProvider(objectType, TestCTypeProbe.class.getClassLoader()).orElseThrow();
    }

    @Test
    public void testStructProbe() throws Exception {
        final CTypeProbe probe = new CTypeProbe(CTypeProbe.Qualifier.STRUCT, "iovec");
        probe.addMember("iov_base", Object.class);
        probe.addMember("iov_len", long.class);
        probe.define("_DEFAULT_SOURCE");
        probe.define("_BSD_SOURCE");
        probe.include("<sys/uio.h>");
        final CTypeProbe.Result result = probe.runProbe(compiler, objectFileProvider);
        assertNotNull(result);
        System.out.println("Probe result for `struct iovec`:");
        System.out.printf(" Overall size = %d%n", Long.valueOf(result.getOverallSize()));
        System.out.printf(" Overall alignment = %d%n", Long.valueOf(result.getOverallAlign()));
        System.out.printf(" iov_base size = %d%n", Long.valueOf(result.getMemberSize("iov_base")));
        System.out.printf(" iov_base offset = %d%n", Long.valueOf(result.getMemberOffset("iov_base")));
        System.out.printf(" iov_len size = %d%n", Long.valueOf(result.getMemberSize("iov_len")));
        System.out.printf(" iov_len offset = %d%n", Long.valueOf(result.getMemberOffset("iov_len")));
        assertTrue(result.getOverallSize() > 0);
        assertTrue(result.getOverallAlign() > 0);
        assertTrue(result.getMemberSize("iov_base") > 0);
        assertTrue(result.getMemberOffset("iov_base") >= 0);
        assertTrue(result.getMemberSize("iov_len") > 0);
        assertTrue(result.getMemberOffset("iov_len") != result.getMemberOffset("iov_base"));
    }

    @Test
    public void testIntProbes() throws Exception {
        final CTypeProbe probe = new CTypeProbe(CTypeProbe.Qualifier.NONE, "int16_t");
        probe.include("<stdint.h>");
        final CTypeProbe.Result result = probe.runProbe(compiler, objectFileProvider);
        assertNotNull(result);
        System.out.println("Probe result for `int16_t`:");
        System.out.printf(" Overall size = %d%n", Long.valueOf(result.getOverallSize()));
        System.out.printf(" Overall alignment = %d%n", Long.valueOf(result.getOverallAlign()));
        System.out.printf(" Signed = %s%n", Boolean.valueOf(result.isSigned()));
        System.out.printf(" Unsigned = %s%n", Boolean.valueOf(result.isUnsigned()));
        System.out.printf(" Floating = %s%n", Boolean.valueOf(result.isFloating()));
        assertEquals(2, result.getOverallSize());
        assertTrue(result.getOverallAlign() > 0);
        assertTrue(result.isSigned());
        assertFalse(result.isUnsigned());
        assertFalse(result.isFloating());
    }
}
