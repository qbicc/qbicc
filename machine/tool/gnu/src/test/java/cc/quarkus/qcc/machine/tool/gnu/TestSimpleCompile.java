package cc.quarkus.qcc.machine.tool.gnu;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;
import cc.quarkus.qcc.machine.file.elf.ElfHeader;
import cc.quarkus.qcc.machine.file.elf.ElfSectionHeaderEntry;
import cc.quarkus.qcc.machine.file.elf.ElfSymbolTableEntry;
import cc.quarkus.qcc.machine.tool.ToolMessageHandler;
import cc.quarkus.qcc.machine.tool.ToolProvider;
import cc.quarkus.qcc.machine.tool.process.InputSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 *
 */
public class TestSimpleCompile {
    @Test
    @EnabledOnOs(OS.LINUX)
    public void testSimpleCompile() throws Exception {
        final Path objectFilePath = Files.createTempFile("temp", ".o");
        final Iterable<GccCompiler> tools = ToolProvider.findAllTools(GccCompiler.class, Platform.HOST_PLATFORM, c -> true,
            TestSimpleCompile.class.getClassLoader());
        final Iterator<GccCompiler> iterator = tools.iterator();
        assertTrue(iterator.hasNext());
        final GccCompiler gccCompiler = iterator.next();
        final GccInvocationBuilder ib = gccCompiler.invocationBuilder();
        ib.setOutputPath(objectFilePath);
        ib.setMessageHandler(new ToolMessageHandler() {
            public void handleMessage(final Level level, final String file, final int line, final int column, final String message) {
                if (level == Level.ERROR) {
                    throw new IllegalStateException("Unexpected error: " + message);
                }
            }
        });
        InputSource.from("extern int foo; int foo = 0x12345678;").transferTo(ib.build());
        assertNotNull(objectFilePath);
        final BinaryBuffer buf = BinaryBuffer.openRead(objectFilePath);
        final ElfHeader elfHeader = ElfHeader.forBuffer(buf);
        final ElfSymbolTableEntry symbol = elfHeader.findSymbol("foo");
        assertNotNull(symbol);
        final long valuePos = symbol.getValue();
        final ElfSectionHeaderEntry section = elfHeader.getSectionHeaderTableEntry(symbol.getLinkedSectionIndex());
        assertEquals(0x12345678, buf.getInt(section.getOffset() + valuePos));
    }
}
