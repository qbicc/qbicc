package org.qbicc.machine.tool.emscripten;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.ToolInvoker;
import org.qbicc.machine.tool.ToolMessageHandler;
import org.qbicc.machine.tool.ToolProvider;
import org.qbicc.machine.tool.ToolUtil;
import org.qbicc.machine.tool.process.InputSource;
import org.junit.jupiter.api.Test;

/**
 *
 */
public class TestSimpleCompile {
    @Test
    public void testSimpleCompile() throws Exception {
        final Path objectFilePath = Files.createTempFile("temp", ".wasm");
        Platform plaf = Platform.parse("wasm-wasi");
        Optional<ObjectFileProvider> of = ObjectFileProvider.findProvider(plaf.getObjectType(), getClass().getClassLoader());
        assumeTrue(of.isPresent());
        Path clang = ToolUtil.findExecutable("emcc");
        assumeTrue(clang != null);
        final Iterable<EmscriptenToolChainImpl> tools = ToolProvider.findAllTools(EmscriptenToolChainImpl.class, plaf, c -> true,
            TestSimpleCompile.class.getClassLoader(), List.of(clang));
        final Iterator<EmscriptenToolChainImpl> iterator = tools.iterator();
        assumeTrue(iterator.hasNext());
        final EmscriptenToolChainImpl gccCompiler = iterator.next();
        final EmscriptenCCompilerInvoker ib = gccCompiler.newCompilerInvoker();
        ib.setOutputPath(objectFilePath);
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final ToolInvoker invoker, final Level level, final String file, final int line, final int column, final String message) {
                if (level == Level.ERROR) {
                    throw new IllegalStateException("Unexpected error: " + message);
                }
            }
        });
        ib.setSource(InputSource.from("extern int foo; int foo = 0x12345678;"));
        ib.invoke();
        assertNotNull(objectFilePath);
        ObjectFile objectFile = of.get().openObjectFile(objectFilePath);
        assertEquals(0x12345678, objectFile.getSymbolValueAsInt("foo"));
    }
}
