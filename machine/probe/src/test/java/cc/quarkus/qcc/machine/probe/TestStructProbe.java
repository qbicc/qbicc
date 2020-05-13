package cc.quarkus.qcc.machine.probe;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.machine.arch.ObjectType;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.gnu.GccCompiler;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestStructProbe {

    @Test
    public void testAProbe() throws Exception {
        final Context dc = new Context(false);
        final StructProbe probe = new StructProbe(StructProbe.Qualifier.STRUCT, "iovec");
        final ObjectType objectType = Platform.HOST_PLATFORM.getObjectType();
        System.out.println("Local object file type: " + objectType);
        final ObjectFileProvider objectFileProvider = ObjectFileProvider.findProvider(objectType, getClass().getClassLoader()).orElseThrow();
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
            final StructProbe.Result res = probe.runProbe(gccCompiler, objectFileProvider);
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
