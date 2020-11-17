package cc.quarkus.qcc.machine.tool.clang;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.Tool;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.ToolUtil;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestSimpleCompile {
    @Test
    public void testSimpleCompile() throws Exception {
        final Path objectFilePath = Files.createTempFile("temp", ".o");
        Platform plaf = Platform.HOST_PLATFORM;
        Optional<ObjectFileProvider> of = ObjectFileProvider.findProvider(plaf.getObjectType(), getClass().getClassLoader());
        assumeTrue(of.isPresent());
        final Iterable<ClangToolChainImpl> tools = ToolProvider.findAllTools(ClangToolChainImpl.class, plaf, c -> true,
            TestSimpleCompile.class.getClassLoader(), List.of(ToolUtil.findExecutable("clang")));
        final Iterator<ClangToolChainImpl> iterator = tools.iterator();
        assumeTrue(iterator.hasNext());
        final ClangToolChainImpl gccCompiler = iterator.next();
        final ClangCCompilerInvoker ib = gccCompiler.newCompilerInvoker();
        ib.setOutputPath(objectFilePath);
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final Tool tool, final Level level, final String file, final int line, final int column, final String message) {
                if (level == Level.ERROR) {
                    throw new IllegalStateException("Unexpected error: " + message);
                }
            }
        });
        ib.setSource(InputSource.from("extern int foo; int foo = 0x12345678;"));
        ib.invoke();
        assertNotNull(objectFilePath);
        ObjectFile objectFile = of.get().openObjectFile(objectFilePath);
        assertEquals(0x12345678, objectFile.getSymbolValueAsLong("foo"));
    }
}
