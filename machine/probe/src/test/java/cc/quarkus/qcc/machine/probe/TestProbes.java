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
        final CTypeProbe.Type struct_iovec = CTypeProbe.Type.builder()
            .setName("iovec")
            .setQualifier(Qualifier.STRUCT)
            .addMember("iov_base")
            .addMember("iov_len")
            .build();
        final CTypeProbe probe = CTypeProbe.builder()
            .addType(struct_iovec)
            .addInclude("<sys/uio.h>")
            .define("_DEFAULT_SOURCE")
            .define("_BSD_SOURCE")
            .build();
        final CTypeProbe.Result probeResult = probe.run(compiler, objectFileProvider);
        assertNotNull(probeResult);
        CTypeProbe.Type.Info result = probeResult.getInfo(struct_iovec);
        System.out.println("Probe result for `struct iovec`:");
        System.out.printf(" Overall size = %d%n", Long.valueOf(result.getSize()));
        System.out.printf(" Overall alignment = %d%n", Long.valueOf(result.getAlign()));
        System.out.printf(" iov_base size = %d%n", Long.valueOf(probeResult.getInfoOfMember(struct_iovec, "iov_base").getSize()));
        long iov_base_offset = probeResult.getInfoOfMember(struct_iovec, "iov_base").getOffset();
        System.out.printf(" iov_base offset = %d%n", Long.valueOf(iov_base_offset));
        System.out.printf(" iov_len size = %d%n", Long.valueOf(probeResult.getInfoOfMember(struct_iovec, "iov_len").getSize()));
        long iov_len_offset = probeResult.getInfoOfMember(struct_iovec, "iov_len").getOffset();
        System.out.printf(" iov_len offset = %d%n", Long.valueOf(iov_len_offset));
        assertTrue(result.getSize() > 0);
        assertTrue(result.getAlign() > 0);
        assertTrue(probeResult.getInfoOfMember(struct_iovec, "iov_base").getSize() > 0);
        assertTrue(iov_base_offset >= 0);
        assertTrue(probeResult.getInfoOfMember(struct_iovec, "iov_len").getSize() > 0);
        assertTrue(iov_len_offset != iov_base_offset);
    }

    @Test
    public void testIntProbes() throws Exception {
        final CTypeProbe.Type int16_t = CTypeProbe.Type.builder().setName("int16_t").build();
        final CTypeProbe probe = CTypeProbe.builder().addType(int16_t).addInclude("<stdint.h>").build();
        final CTypeProbe.Result probeResult = probe.run(compiler, objectFileProvider);
        assertNotNull(probeResult);
        CTypeProbe.Type.Info result = probeResult.getInfo(int16_t);
        System.out.println("Probe result for `int16_t`:");
        System.out.printf(" Overall size = %d%n", Long.valueOf(result.getSize()));
        System.out.printf(" Overall alignment = %d%n", Long.valueOf(result.getAlign()));
        System.out.printf(" Signed = %s%n", Boolean.valueOf(result.isSigned()));
        System.out.printf(" Unsigned = %s%n", Boolean.valueOf(result.isUnsigned()));
        System.out.printf(" Floating = %s%n", Boolean.valueOf(result.isFloating()));
        assertEquals(2, result.getSize());
        assertTrue(result.getAlign() > 0);
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
