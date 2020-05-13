package cc.quarkus.qcc.machine.file.elf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import cc.quarkus.qcc.machine.arch.ObjectType;
import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;
import cc.quarkus.qcc.machine.object.ObjectFile;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import io.smallrye.common.constraint.Assert;

/**
 * The object file provider implementation for ELF files.
 */
public class ElfObjectFileProvider implements ObjectFileProvider {
    public ElfObjectFileProvider() {}

    public ObjectFile openObjectFile(final Path path) throws IOException {
        final BinaryBuffer buffer = BinaryBuffer.openRead(path);
        final ElfHeader elfHeader = ElfHeader.forBuffer(buffer);
        return new ObjectFile() {
            public int getSymbolValueAsInt(final String name) {
                final ElfSymbolTableEntry symbol = findSymbol(name);
                final long size = symbol.getValueSize();
                final int linkedSection = symbol.getLinkedSectionIndex();
                final ElfSectionHeaderEntry codeSection = elfHeader.getSectionHeaderTableEntry(linkedSection);
                if (codeSection.getType() == Elf.Section.Type.Std.NO_BITS) {
                    // bss
                    return 0;
                }
                if (size == 4) {
                    return elfHeader.getBackingBuffer().getInt(codeSection.getOffset() + symbol.getValue());
                } else {
                    throw new IllegalArgumentException("Unexpected size " + size);
                }
            }

            public long getSymbolValueAsLong(final String name) {
                final ElfSymbolTableEntry symbol = findSymbol(name);
                final long size = symbol.getValueSize();
                final int linkedSection = symbol.getLinkedSectionIndex();
                final ElfSectionHeaderEntry codeSection = elfHeader.getSectionHeaderTableEntry(linkedSection);
                if (codeSection.getType() == Elf.Section.Type.Std.NO_BITS) {
                    // bss
                    return 0;
                }
                if (size == 8) {
                    return elfHeader.getBackingBuffer().getLong(codeSection.getOffset() + symbol.getValue());
                } else if (size == 4) {
                    return elfHeader.getBackingBuffer().getIntUnsigned(codeSection.getOffset() + symbol.getValue());
                } else {
                    throw new IllegalArgumentException("Unexpected size " + size);
                }
            }

            public String getSymbolValueAsUtfString(final String name) {
                throw Assert.unsupported();
            }

            public long getSymbolSize(final String name) {
                return findSymbol(name).getValueSize();
            }

            private ElfSymbolTableEntry findSymbol(final String name) {
                final ElfSymbolTableEntry symbol = elfHeader.findSymbol(name);
                if (symbol == null) {
                    throw new NoSuchElementException("Symbol \"" + name + "\" not found in object file");
                }
                return symbol;
            }

            public void close() throws IOException {
                buffer.close();
            }
        };
    }

    public ObjectType getObjectType() {
        return ObjectType.ELF;
    }
}
