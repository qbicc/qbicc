package org.qbicc.machine.file.elf;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.NoSuchElementException;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import org.qbicc.machine.object.ObjectFileProvider;
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
            public int getSymbolValueAsByte(final String name) {
                final ElfSymbolTableEntry symbol = findSymbol(name);
                final long size = symbol.getValueSize();
                final int linkedSection = symbol.getLinkedSectionIndex();
                final ElfSectionHeaderEntry codeSection = elfHeader.getSectionHeaderTableEntry(linkedSection);
                if (codeSection.getType() == Elf.Section.Type.Std.NO_BITS) {
                    // bss
                    return 0;
                }
                if (size == 1) {
                    return elfHeader.getBackingBuffer().getByteUnsigned(codeSection.getOffset() + symbol.getValue());
                } else {
                    throw new IllegalArgumentException("Unexpected size " + size);
                }
            }

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

            public byte[] getSymbolAsBytes(final String name, final int size) {
                final ElfSymbolTableEntry symbol = findSymbol(name);
                final long symSize = symbol.getValueSize();
                final int linkedSection = symbol.getLinkedSectionIndex();
                final ElfSectionHeaderEntry codeSection = elfHeader.getSectionHeaderTableEntry(linkedSection);
                final byte[] array = new byte[size];
                if (codeSection.getType() == Elf.Section.Type.Std.NO_BITS) {
                    // bss
                    return array;
                } else {
                    elfHeader.getBackingBuffer().getBytes(codeSection.getOffset() + symbol.getValue(), array);
                    return array;
                }
            }

            public String getSymbolValueAsUtfString(final String name) {
                throw Assert.unsupported();
            }

            public long getSymbolSize(final String name) {
                return findSymbol(name).getValueSize();
            }

            public ByteOrder getByteOrder() {
                return buffer.getByteOrder();
            }

            public Cpu getCpu() {
                return elfHeader.getMachine().toCpu();
            }

            public ObjectType getObjectType() {
                return ObjectType.ELF;
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
