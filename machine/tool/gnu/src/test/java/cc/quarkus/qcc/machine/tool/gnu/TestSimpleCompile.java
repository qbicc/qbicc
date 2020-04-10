package cc.quarkus.qcc.machine.tool.gnu;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.Iterator;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;
import cc.quarkus.qcc.machine.file.elf.ElfHeader;
import cc.quarkus.qcc.machine.file.elf.ElfSectionHeaderEntry;
import cc.quarkus.qcc.machine.file.elf.ElfSymbolTableEntry;
import cc.quarkus.qcc.machine.tool.CompilationResult;
import cc.quarkus.qcc.machine.tool.InputSource;
import cc.quarkus.qcc.machine.tool.ToolProvider;
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
        final Context dc = new Context(false);
        final CompilationResult result = dc.run(() -> {
            final Iterable<GccCompiler> tools = ToolProvider.findAllTools(GccCompiler.class,
                    TestSimpleCompile.class.getClassLoader());
            final Iterator<GccCompiler> iterator = tools.iterator();
            assertTrue(iterator.hasNext());
            final GccCompiler gccCompiler = iterator.next();
            assertFalse(iterator.hasNext());
            final GccInvocationBuilder ib = gccCompiler.invocationBuilder();
            ib.setInputSource(new InputSource.String("extern int foo; int foo = 0x12345678;"));
            final CompilationResult r = ib.invoke();
            assertNotNull(r);
            assertEquals(0, Context.errors());
            assertEquals(0, Context.warnings());
            return r;
        });
        final Path objectFilePath = result.getObjectFilePath();
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
