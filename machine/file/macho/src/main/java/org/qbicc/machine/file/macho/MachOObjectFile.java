package org.qbicc.machine.file.macho;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.qbicc.machine.arch.Cpu;
import org.qbicc.machine.arch.ObjectType;
import org.qbicc.machine.file.bin.BinaryBuffer;
import org.qbicc.machine.object.ObjectFile;
import io.smallrye.common.constraint.Assert;

/**
 * A Mach-O object file.
 */
public final class MachOObjectFile implements ObjectFile {
    private final BinaryBuffer buffer;
    private final Header header;
    private final EnumMap<MachO.LoadCommand, List<LoadCommand>> commands;
    private final List<SymTab> symTabs = new ArrayList<>();
    private final Map<String, NList> syms = new HashMap<>();
    private final Map<String, Map<String, Section>> segmentsAndSections = new HashMap<>();
    private final List<Section> sections = new ArrayList<>();

    public MachOObjectFile(final BinaryBuffer buffer) throws IOException {
        int magic = buffer.getInt(0);
        if (magic == MachO.MH_CIGAM_32 || magic == MachO.MH_CIGAM_64) {
            buffer.setByteOrder(opposite(buffer.getByteOrder()));
            magic = buffer.getInt(0);
        } else if (magic == MachO.MH_MAGIC_32 || magic == MachO.MH_MAGIC_64) {
            // OK
        } else {
            throw new IOException("Invalid magic number");
        }
        this.buffer = buffer;
        this.header = new Header(buffer);
        // load commands
        commands = new EnumMap<>(MachO.LoadCommand.class);
        long offs = magic == MachO.MH_MAGIC_32 ? 28 : 32;
        List<Segment> segments = new ArrayList<>();
        for (int i = 0; i < header.nCmds; i ++) {
            final LoadCommand cmd = new LoadCommand(buffer, offs);
            commands.computeIfAbsent(cmd.command, c -> new ArrayList<>()).add(cmd);
            if (cmd.command == MachO.LoadCommand.LC_SYMTAB) {
                symTabs.add(new SymTab(buffer, cmd));
            } else if (cmd.command == MachO.LoadCommand.LC_SEGMENT) {
                final Segment segment = new Segment(buffer, offs + 8, false);
                segments.add(segment);
                final Map<String, Section> subMap = segmentsAndSections.computeIfAbsent(segment.name, n -> new HashMap<>());
                long subOffs = offs + 56;
                for (int j = 0; j < segment.numSections; j ++) {
                    final Section section = new Section(buffer, subOffs, false);
                    subMap.put(section.name, section);
                    sections.add(section);
                    subOffs += 68;
                }
            } else if (cmd.command == MachO.LoadCommand.LC_SEGMENT_64) {
                final Segment segment = new Segment(buffer, offs + 8, true);
                segments.add(segment);
                final Map<String, Section> subMap = segmentsAndSections.computeIfAbsent(segment.name, n -> new HashMap<>());
                long subOffs = offs + 72;
                for (int j = 0; j < segment.numSections; j ++) {
                    final Section section = new Section(buffer, subOffs, true);
                    subMap.put(section.name, section);
                    sections.add(section);
                    subOffs += 80;
                }
            }

            offs += cmd.size;
        }
        for (SymTab symTab : symTabs) {
            final long nSyms = symTab.nSyms;
            long offset = symTab.offset;
            final long strOff = symTab.strOff;
            final long strSize = symTab.strSize;
            for (long i = 0; i < nSyms; i ++) {
                NList nlist = new NList(buffer, offset, header.abi64);
                final long sti = nlist.stringTableIndex;
                if (sti != 0) {
                    final BinaryBuffer.ReadingIterator iter = buffer.readingIterator(strOff + sti);
                    StringBuilder b = new StringBuilder();
                    int cp = iter.getCodePoint();
                    if (cp == '_') {
                        // drop it
                        cp = iter.getCodePoint();
                    } else if (cp == 0) {
                        continue;
                    }
                    do {
                        b.appendCodePoint(cp);
                        cp = iter.getCodePoint();
                    } while (cp != 0);
                    syms.put(b.toString(), nlist);
                }
                offset += nlist.size;
            }
        }
    }

    public int getSymbolValueAsByte(final String name) {
        final NList symbol = requireSymbol(name);
        final long value = symbol.value;
        if (symbol.type == MachO.NList.Type.BSS) {
            // implicitly zero
            return 0;
        } else if (symbol.type == MachO.NList.Type.SECT) {
            // offset into data
            final Section section = sections.get(symbol.section - 1);
            if (section.name.equals("__common")) {
                return 0;
            }
            return buffer.getByteUnsigned(section.fileOffset + symbol.value);
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + symbol.type);
        }
    }

    public int getSymbolValueAsInt(final String name) {
        final NList symbol = requireSymbol(name);
        final long value = symbol.value;
        if (symbol.type == MachO.NList.Type.BSS) {
            // implicitly zero
            return 0;
        } else if (symbol.type == MachO.NList.Type.SECT) {
            // offset into data
            final Section section = sections.get(symbol.section - 1);
            if (section.name.equals("__common")) {
                return 0;
            }
            return buffer.getInt(section.fileOffset + symbol.value);
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + symbol.type);
        }
    }

    public long getSymbolValueAsLong(final String name) {
        final NList symbol = requireSymbol(name);
        final long value = symbol.value;
        if (symbol.type == MachO.NList.Type.BSS) {
            // implicitly zero
            return 0;
        } else if (symbol.type == MachO.NList.Type.SECT) {
            // offset into data
            final Section section = sections.get(symbol.section - 1);
            if (section.name.equals("__common")) {
                return 0;
            }
            return buffer.getLong(section.fileOffset + symbol.value);
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + symbol.type);
        }
    }

    public byte[] getSymbolAsBytes(final String name, final int size) {
        final byte[] array = new byte[size];
        final NList symbol = requireSymbol(name);
        final long value = symbol.value;
        if (symbol.type == MachO.NList.Type.BSS) {
            // implicitly zero
            return array;
        } else if (symbol.type == MachO.NList.Type.SECT) {
            // offset into data
            final Section section = sections.get(symbol.section - 1);
            if (section.name.equals("__common")) {
                return array;
            }
            buffer.getBytes(section.fileOffset + symbol.value, array);
            return array;
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + symbol.type);
        }
    }

    public String getSymbolValueAsUtfString(final String name, final int nbytes) {
        final NList symbol = requireSymbol(name);
        final long value = symbol.value;
        if (symbol.type == MachO.NList.Type.BSS) {
            // implicitly zero
            return "";
        } else if (symbol.type == MachO.NList.Type.SECT) {
            // offset into data
            final Section section = sections.get(symbol.section - 1);
            if (section.name.equals("__common")) {
                return "";
            }
            final byte[] array = new byte[nbytes];
            buffer.getBytes(section.fileOffset + symbol.value, array);
            return new String(array, StandardCharsets.UTF_8);
        } else {
            throw new IllegalArgumentException("Unexpected symbol type " + symbol.type);
        }
    }

    public long getSymbolSize(final String name) {
        return requireSymbol(name).size;
    }

    NList requireSymbol(final String name) {
        final NList nList = syms.get(name);
        if (nList == null) {
            throw new IllegalArgumentException("No symbol named `" + name + "` found");
        }
        return nList;
    }

    Section requireSection(final String segmentName, final String sectionName) {
        final Section section = segmentsAndSections.getOrDefault(segmentName, Map.of()).get(sectionName);
        if (section == null) {
            throw new IllegalArgumentException("No section named `" + sectionName + "` in segment `" + segmentName + "` found");
        }
        return section;
    }

    public ByteOrder getByteOrder() {
        return buffer.getByteOrder();
    }

    public Cpu getCpu() {
        return header.cpuType.toCpu(header.abi64);
    }

    public ObjectType getObjectType() {
        return ObjectType.MACH_O;
    }

    @Override
    public org.qbicc.machine.object.Section getSection(String name) {
        final Section section = sections.stream().filter(s -> s.name.equals(name)).findFirst().orElse(null);
        if (section == null) {
            return null;
        }
        return new org.qbicc.machine.object.Section() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public ByteBuffer getSectionContent() {
                return buffer.getBuffer(section.fileOffset, section.size);
            }
        };
    }

    @Override
    public String getRelocationSymbolForSymbolValue(String symbol) {
        return null;
    }

    @Override
    public String getStackMapSectionName() {
        return "__llvm_stackmaps";
    }

    public void close() {
        buffer.close();
    }

    static ByteOrder opposite(ByteOrder order) {
        return order == ByteOrder.BIG_ENDIAN ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN;
    }

    static final class Header {
        final MachO.CpuType cpuType;
        final boolean abi64;
        final MachO.FileType fileType;
        final int nCmds;
        final int sizeOfCmds;
        final Set<MachO.Flag> flags;

        Header(BinaryBuffer buffer) {
            final int cpuTypeInt = buffer.getInt(4);
            abi64 = (cpuTypeInt & MachO.CPU_ARCH_ABI64) != 0;
            cpuType = MachO.CpuType.forValue(cpuTypeInt & 0xffffff);
            fileType = MachO.FileType.forValue(buffer.getInt(16));
            nCmds = buffer.getInt(16);
            sizeOfCmds = buffer.getInt(20);
            flags = setOfBits(MachO.Flag.class, buffer.getIntUnsigned(24), MachO.Flag::forValue);
        }
    }

    static final class LoadCommand {
        final long offset;
        final MachO.LoadCommand command;
        final long size;

        LoadCommand(final BinaryBuffer buffer, final long offset) {
            this.offset = offset;
            command = MachO.LoadCommand.forValue(buffer.getInt(offset));
            size = buffer.getIntUnsigned(offset + 4);
        }
    }

    static final class SymTab {
        final long offset;
        final long nSyms;
        final long strOff;
        final long strSize;

        SymTab(BinaryBuffer buffer, LoadCommand cmd) {
            offset = buffer.getIntUnsigned(cmd.offset + 8);
            nSyms = buffer.getIntUnsigned(cmd.offset + 12);
            strOff = buffer.getIntUnsigned(cmd.offset + 16);
            strSize = buffer.getIntUnsigned(cmd.offset + 20);
        }
    }

    static final class NList {
        final long offset;
        final int size;
        final long stringTableIndex;
        final boolean privateExtern;
        final boolean external;
        final boolean stab;
        final MachO.NList.Type type;
        final int section;
        final boolean weakRef;
        final boolean weakDef;
        final long value;

        NList(BinaryBuffer buffer, long offset, boolean is64) {
            this.offset = offset;
            size = is64 ? 16 : 12;
            stringTableIndex = buffer.getIntUnsigned(offset);
            int n_type = buffer.getByteUnsigned(offset + 4);
            privateExtern = (n_type & 0x10) != 0;
            external = (n_type & 0x01) != 0;
            stab = (n_type & 0xe0) != 0;
            type = stab ? MachO.NList.Type.UNDEF : MachO.NList.Type.forValue((n_type >> 1) & 0x7);
            section = buffer.getByteUnsigned(offset + 5);
            int n_desc = buffer.getShortUnsigned(offset + 6);
            weakRef = (n_desc & 0x40) != 0;
            weakDef = (n_desc & 0x80) != 0;
            value = is64 ? buffer.getLong(offset + 8) : buffer.getIntUnsigned(offset + 8);
        }
    }

    static final class Segment {
        final String name;
        final long address;
        final long segmentSize;
        final long segmentOffset;
        final long fileSize;
        final long numSections;

        Segment(BinaryBuffer buffer, long offset, boolean is64) {
            name = fromBytes(buffer, offset, 16);
            address = is64 ? buffer.getLong(offset + 16) : buffer.getIntUnsigned(offset + 16);
            segmentSize = is64 ? buffer.getLong(offset + 24) : buffer.getIntUnsigned(offset + 20);
            segmentOffset = is64 ? buffer.getLong(offset + 32) : buffer.getIntUnsigned(offset + 24);
            fileSize = is64 ? buffer.getLong(offset + 40) : buffer.getIntUnsigned(offset + 28);
            numSections = is64 ? buffer.getIntUnsigned(offset + 56) : buffer.getIntUnsigned(offset + 36);
        }

    }

    static final class Section {
        final String name;
        final long address;
        final long size;
        final long fileOffset;
        final long align;
        final long relOff;

        Section(BinaryBuffer buffer, long offset, boolean is64) {
            name = fromBytes(buffer, offset, 16);
            address = is64 ? buffer.getLong(offset + 32) : buffer.getIntUnsigned(offset + 32);
            size = is64 ? buffer.getLong(offset + 40) : buffer.getIntUnsigned(offset + 36);
            fileOffset = is64 ? buffer.getIntUnsigned(offset + 48) : buffer.getIntUnsigned(offset + 40);
            align = is64 ? buffer.getIntUnsigned(offset + 52) : buffer.getIntUnsigned(offset + 44);
            relOff = is64 ? buffer.getIntUnsigned(offset + 56) : buffer.getIntUnsigned(offset + 48);
        }
    }

    static int indexOf(byte[] b, int val) {
        for (int i = 0; i < b.length; i ++) {
            if (b[i] == val) {
                return i;
            }
        }
        return -1;
    }

    static String fromBytes(BinaryBuffer buffer, long offset, int size) {
        final byte[] bytes = new byte[size];
        buffer.getBytes(offset, bytes);
        return fromBytes(bytes);
    }

    static String fromBytes(byte[] bytes) {
        int len = indexOf(bytes, 0);
        if (len == -1) {
            len = 16;
        }
        return new String(bytes, 0, len, StandardCharsets.UTF_8);
    }

    static <E extends Enum<E>> EnumSet<E> setOfBits(Class<E> type, long bits, IntFunction<E> decoder) {
        final EnumSet<E> set = EnumSet.noneOf(type);
        while (bits != 0) {
            final long lob = Long.lowestOneBit(bits);
            bits &= ~lob;
            set.add(decoder.apply(Long.numberOfTrailingZeros(lob)));
        }
        return set;
    }
}
