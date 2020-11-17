package cc.quarkus.qcc.machine.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import cc.quarkus.qcc.machine.arch.ObjectType;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestProbes {

    static CToolChain compiler;
    static ObjectFileProvider objectFileProvider;

    @BeforeAll
    public static void setUpCompiler() {
        final Iterable<CToolChain> tools = ToolProvider.findAllTools(CToolChain.class, Platform.HOST_PLATFORM, s -> true,
            TestProbes.class.getClassLoader(), List.of(ToolUtil.findExecutable("cc"), ToolUtil.findExecutable("gcc")));
        final Iterator<CToolChain> iterator = tools.iterator();
        assertTrue(iterator.hasNext());
        compiler = iterator.next();
        final ObjectType objectType = Platform.HOST_PLATFORM.getObjectType();
        System.out.println("Local object file type: " + objectType);
        objectFileProvider = ObjectFileProvider.findProvider(objectType, TestProbes.class.getClassLoader()).orElseThrow();
    }

    @Test
    public void testStructProbe() throws Exception {
        final CTypeProbe probe = new CTypeProbe(Qualifier.STRUCT, "iovec");
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
        final CTypeProbe probe = new CTypeProbe(Qualifier.NONE, "int16_t");
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

    @Test
    public void testSymbolProbe() throws Exception {
        final CConstantProbe probe = new CConstantProbe("INT8_MAX", Qualifier.NONE, "int8_t", 1);
        probe.include("<stdint.h>");
        final CConstantProbe.Result result = probe.runProbe(compiler, objectFileProvider);
        assertNotNull(result);
        assertTrue(result.isDefined());
        assertArrayEquals(new byte[] { Byte.MAX_VALUE }, result.getValue());
    }
}
