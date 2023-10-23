package org.qbicc.machine.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;
import java.util.List;

import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.tool.ToolProvider;
import org.qbicc.machine.tool.ToolUtil;
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
        final ObjectType objectType = Platform.HOST_PLATFORM.objectType();
        System.out.println("Local object file type: " + objectType);
        objectFileProvider = ObjectFileProvider.findProvider(objectType, TestProbes.class.getClassLoader()).orElseThrow();
    }

    @Test
    public void testStructProbe() throws Exception {
        final CProbe.Type struct_iovec = CProbe.Type.builder()
            .setName("iovec")
            .setQualifier(Qualifier.STRUCT)
            .addMember("iov_base")
            .addMember("iov_len")
            .build();
        final CProbe probe = CProbe.builder()
            .define("_DEFAULT_SOURCE")
            .define("_BSD_SOURCE")
            .include("<sys/uio.h>")
            .probeType(struct_iovec)
            .build();
        final CProbe.Result probeResult = probe.run(compiler, objectFileProvider, null);
        assertNotNull(probeResult);
        CProbe.Type.Info result = probeResult.getTypeInfo(struct_iovec);
        System.out.println("Probe result for `struct iovec`:");
        System.out.printf(" Overall size = %d%n", Long.valueOf(result.getSize()));
        System.out.printf(" Overall alignment = %d%n", Long.valueOf(result.getAlign()));
        System.out.printf(" iov_base size = %d%n", Long.valueOf(probeResult.getTypeInfoOfMember(struct_iovec, "iov_base").getSize()));
        long iov_base_offset = probeResult.getTypeInfoOfMember(struct_iovec, "iov_base").getOffset();
        System.out.printf(" iov_base offset = %d%n", Long.valueOf(iov_base_offset));
        System.out.printf(" iov_len size = %d%n", Long.valueOf(probeResult.getTypeInfoOfMember(struct_iovec, "iov_len").getSize()));
        long iov_len_offset = probeResult.getTypeInfoOfMember(struct_iovec, "iov_len").getOffset();
        System.out.printf(" iov_len offset = %d%n", Long.valueOf(iov_len_offset));
        assertTrue(result.getSize() > 0);
        assertTrue(result.getAlign() > 0);
        assertTrue(probeResult.getTypeInfoOfMember(struct_iovec, "iov_base").getSize() > 0);
        assertTrue(iov_base_offset >= 0);
        assertTrue(probeResult.getTypeInfoOfMember(struct_iovec, "iov_len").getSize() > 0);
        assertTrue(iov_len_offset != iov_base_offset);
    }

    @Test
    public void testIntProbes() throws Exception {
        final CProbe.Type int16_t = CProbe.Type.builder().setName("int16_t").build();
        final CProbe probe = CProbe.builder().include("<stdint.h>").probeType(int16_t).build();
        final CProbe.Result probeResult = probe.run(compiler, objectFileProvider, null);
        assertNotNull(probeResult);
        CProbe.Type.Info result = probeResult.getTypeInfo(int16_t);
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
        final CProbe probe = CProbe.builder().include("<stdint.h>").probeConstant("INT8_MAX").build();
        final CProbe.Result result = probe.run(compiler, objectFileProvider, null);
        assertNotNull(result);
        assertTrue(result.getConstantInfo("INT8_MAX").isDefined());
        assertEquals(Byte.MAX_VALUE, result.getConstantInfo("INT8_MAX").getValueAsInt());
    }
}
