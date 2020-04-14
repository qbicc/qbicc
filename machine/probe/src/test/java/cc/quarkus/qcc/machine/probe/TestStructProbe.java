package cc.quarkus.qcc.machine.probe;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;

import cc.quarkus.qcc.machine.arch.Platform;
import org.junit.jupiter.api.Test;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.gnu.GccCompiler;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 *
 */
public class TestStructProbe {

    @Test
    @EnabledOnOs(OS.LINUX)
    public void testAProbe() throws Exception {
        final Context dc = new Context(false);
        final StructProbe probe = new StructProbe(StructProbe.Qualifier.STRUCT, "iovec");
        probe.addMember("iov_base", Object.class);
        probe.addMember("iov_len", long.class);
        probe.define("_DEFAULT_SOURCE");
        probe.define("_BSD_SOURCE");
        probe.include("<sys/uio.h>");
        final StructProbe.Result result = dc.run(() -> {
            final Iterable<GccCompiler> tools = ToolProvider.findAllTools(GccCompiler.class, Platform.HOST_PLATFORM, s -> true,
                    TestStructProbe.class.getClassLoader());
            final Iterator<GccCompiler> iterator = tools.iterator();
            assertTrue(iterator.hasNext());
            final GccCompiler gccCompiler = iterator.next();
            assertFalse(iterator.hasNext());
            final StructProbe.Result res = probe.runProbe(gccCompiler);
            Context.dump(System.out);
            return res;
        });
        assertNotNull(result);
        System.out.println("Probe result:");
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
}
