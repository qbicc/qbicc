package org.qbicc.machine.file.elf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.qbicc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class ElfHeader {
    final BinaryBuffer backingBuffer;
    final ArrayList<ElfSectionHeaderEntry> sectionEntries = new ArrayList<>(0);
    final ArrayList<ElfProgramHeaderEntry> programEntries = new ArrayList<>(0);
    final Map<String, ElfSectionHeaderEntry> sectionCache = new HashMap<>();
    final ArrayList<ElfSymbolTableEntry> staticSymbols = new ArrayList<>(0);
    final ArrayList<ElfSymbolTableEntry> dynamicSymbols = new ArrayList<>(0);
    final Map<String, ElfSymbolTableEntry> symbolCache = new HashMap<>();
    final MappedBitSet<Elf.Flag> flags;

    ElfHeader(final BinaryBuffer backingBuffer, final long flagsOffset) {
        this.backingBuffer = backingBuffer;
        this.flags = MappedBitSet.map32Bits(backingBuffer, flagsOffset, Elf.Flag.class, this::decodeFlag, this::maskFlags);
    }

    public static ElfHeader forBuffer(BinaryBuffer buffer, Elf.Class elfClass) {
        if (elfClass == Elf.Class.Std._32) {
            return new _32(buffer);
        } else if (elfClass == Elf.Class.Std._64) {
            return new _64(buffer);
        } else {
            throw new IllegalArgumentException("Unsupported ELF class " + elfClass);
        }
    }

    public static ElfHeader forBuffer(BinaryBuffer buffer) {
        final int dataVal = buffer.getByteUnsigned(5);
        final Elf.Data.Std elfData = Elf.Data.Std.forValue(dataVal);
        buffer.setByteOrder(elfData.byteOrder());
        final int classVal = buffer.getByteUnsigned(4);
        final Elf.Class elfClass = Elf.Class.Std.forValue(classVal);
        final ElfHeader elfHeader = forBuffer(buffer, elfClass == null ? Elf.Class.unknown(classVal) : elfClass);
        elfHeader.sectionEntries.ensureCapacity(elfHeader.getSectionHeaderTableEntryCount());
        elfHeader.programEntries.ensureCapacity(elfHeader.getProgramHeaderTableEntryCount());
        return elfHeader;
    }

    public String getStringFromSection(int sectionIndex, long stringOffset) {
        final ElfSectionHeaderEntry entry = getSectionHeaderTableEntry(sectionIndex);
        if (entry.getType() != Elf.Section.Type.Std.STR_TAB) {
            return null;
        }
        long charSize = entry.getFixedEntrySize();
        if (charSize == 0) {
            charSize = 1;
        }
        if (charSize != 1 && charSize != 2) {
            throw new UnsupportedOperationException("Unsupported character size");
        }
        if (stringOffset > entry.getSize()) {
            return null;
        }
        final BinaryBuffer.ReadingIterator iter = getBackingBuffer().readingIterator(entry.getOffset() + stringOffset);
        StringBuilder b = new StringBuilder();
        final long end = entry.getOffset() + entry.getSize();
        while (iter.peekByte() != 0 && iter.position() < end) {
            if (charSize == 1) {
                b.appendCodePoint(iter.getCodePoint());
            } else {
                assert charSize == 2;
                b.append((char) iter.getShort());
            }
        }
        return b.toString();
    }

    public String getString(long stringOffset) {
        return getStringFromSection(getStringTableSectionHeaderIndex(), stringOffset);
    }

    public boolean stringEquals(int sectionIndex, long stringOffset, String expected) {
        final ElfSectionHeaderEntry entry = getSectionHeaderTableEntry(sectionIndex);
        if (entry.getType() != Elf.Section.Type.Std.STR_TAB) {
            return false;
        }
        long charSize = entry.getFixedEntrySize();
        if (charSize == 0) {
            charSize = 1;
        }
        if (charSize != 1 && charSize != 2) {
            throw new UnsupportedOperationException("Unsupported character size");
        }
        if (stringOffset > entry.getSize()) {
            return false;
        }
        final BinaryBuffer.ReadingIterator iter = getBackingBuffer().readingIterator(entry.getOffset() + stringOffset);
        int i = 0;
        final long end = entry.getOffset() + entry.getSize();
        while (iter.peekByte() != 0 && iter.position() < end && i < expected.length()) {
            if (charSize == 1) {
                final int ecp = expected.codePointAt(i);
                if (iter.getCodePoint() != ecp) {
                    return false;
                }
                i += Character.charCount(ecp);
            } else {
                assert charSize == 2;
                if (iter.getShortUnsigned() != expected.charAt(i)) {
                    return false;
                }
                i++;
            }
        }
        return iter.peekByte() == 0 && i == expected.length();
    }

    public boolean stringEquals(long stringOffset, String expected) {
        return stringEquals(getStringTableSectionHeaderIndex(), stringOffset, expected);
    }

    public BinaryBuffer getBackingBuffer() {
        return backingBuffer;
    }

    public boolean checkMagic() {
        final BinaryBuffer bb = backingBuffer;
        return bb.getByteUnsigned(0) == 0x7F &&
                bb.getByteUnsigned(1) == 'E' &&
                bb.getByteUnsigned(2) == 'L' &&
                bb.getByteUnsigned(3) == 'F';
    }

    public void setMagic() {
        final BinaryBuffer bb = backingBuffer;
        bb.putByte(3, 'F');
        bb.putByte(2, 'L');
        bb.putByte(1, 'E');
        bb.putByte(0, 0x7F);
    }

    public abstract Elf.Class getElfClass();

    public boolean checkElfClass() {
        final int val = backingBuffer.getByteUnsigned(4);
        return Elf.Class.forValue(val) == getElfClass();
    }

    public void setElfClass() {
        backingBuffer.putByte(4, getElfClass().getValue());
    }

    public Elf.Data getElfData() {
        final int val = backingBuffer.getByteUnsigned(5);
        return Elf.Data.forValue(val);
    }

    public void setElfData(Elf.Data elfData) {
        backingBuffer.putByte(5, elfData.getValue());
    }

    public int getVersion() {
        return backingBuffer.getByteUnsigned(6);
    }

    public void setVersion(int version) {
        backingBuffer.putByte(6, version);
    }

    public Elf.OsAbi getOsAbi() {
        final int val = backingBuffer.getByteUnsigned(7);
        return Elf.OsAbi.forValue(val);
    }

    public void setOsAbi(Elf.OsAbi osAbi) {
        backingBuffer.putByte(7, osAbi.getValue());
    }

    public int getAbiVersion() {
        return backingBuffer.getByteUnsigned(8);
    }

    public void setAbiVersion(int version) {
        backingBuffer.putByte(8, version);
    }

    // the following methods must be called with the correct byte order set on backingBuffer

    public Elf.Type getType() {
        final int val = backingBuffer.getShortUnsigned(0x10);
        return Elf.Type.forValue(val);
    }

    public void setType(Elf.Type type) {
        backingBuffer.putShort(0x10, type.getValue());
    }

    public Elf.Machine getMachine() {
        final int val = backingBuffer.getShortUnsigned(0x12);
        final Elf.Machine machine = Elf.Machine.Std.forValue(val);
        return machine == null ? Elf.Machine.unknown(val) : machine;
    }

    public void setMachine(Elf.Machine machine) {
        backingBuffer.putShort(0x12, machine.getValue());
    }

    Elf.Flag decodeFlag(int flag) {
        return getMachine().decodeFlag(flag);
    }

    long maskFlags(long flags) {
        return getMachine().maskFlags(flags);
    }

    public int getMachineSpecificValue() {
        return getMachine().getSpecificValue(flags.getRawValue());
    }

    public void setMachineSpecificValue(int value) {
        flags.setRawValue(getMachine().mergeSpecificValue(flags.getRawValue(), value));
    }

    public int getFileVersion() {
        return backingBuffer.getInt(0x14);
    }

    public void setFileVersion(int version) {
        backingBuffer.putInt(0x14, version);
    }

    public abstract int getExpectedSize();

    public abstract int getActualSize();

    public boolean checkSize() {
        return getExpectedSize() == getActualSize();
    }

    public abstract void setSize();

    public abstract long getEntryPoint();

    public abstract void setEntryPoint(long entryPoint);

    public abstract long getProgramHeaderTableOffset();

    public abstract void setProgramHeaderTableOffset(long offset);

    public abstract long getSectionHeaderTableOffset();

    public abstract void setSectionHeaderTableOffset(long offset);

    public Set<Elf.Flag> getFlags() {
        return flags;
    }

    public boolean checkProgramHeaderTableEntrySize() {
        return getExpectedProgramHeaderTableEntrySize() == getActualProgramHeaderTableEntrySize();
    }

    public abstract void setProgramHeaderTableEntrySize();

    public abstract int getExpectedProgramHeaderTableEntrySize();

    public abstract int getActualProgramHeaderTableEntrySize();

    public abstract int getProgramHeaderTableEntryCount();

    public abstract void setProgramHeaderTableEntryCount(int count);

    public boolean checkSectionHeaderTableEntrySize() {
        return getExpectedSectionHeaderTableEntrySize() == getActualSectionHeaderTableEntrySize();
    }

    public abstract void setSectionHeaderTableEntrySize();

    public abstract int getExpectedSectionHeaderTableEntrySize();

    public abstract int getActualSectionHeaderTableEntrySize();

    public abstract int getSectionHeaderTableEntryCount();

    public abstract void setSectionHeaderTableEntryCount(int count);

    public abstract int getStringTableSectionHeaderIndex();

    public ElfSectionHeaderEntry getStringTableSectionHeaderEntry() {
        return getSectionHeaderTableEntry(getStringTableSectionHeaderIndex());
    }

    public abstract void setStringTableSectionHeaderIndex(int index);

    public ElfRelocationTableEntry appendRelocation(ElfSectionHeaderEntry relocationSection, BinaryBuffer.WritingIterator iter) {
        final Elf.Section.Type sectionType = relocationSection.getType();
        if (sectionType != Elf.Section.Type.Std.REL && sectionType != Elf.Section.Type.Std.REL_A) {
            throw new IllegalArgumentException("Invalid relocation section type " + sectionType);
        }
        final ElfRelocationTableEntry entry = constructRelocationTableEntry(relocationSection, iter.position());
        final long entrySize = relocationSection.getFixedEntrySize();
        relocationSection.setSize(relocationSection.getSize() + entrySize);
        iter.skip(entrySize);
        return entry;
    }

    public void initialize() {
        setMagic();
        setElfClass();
        setVersion(1);
        setFileVersion(1);
        setSize();
        setProgramHeaderTableEntrySize();
        setSectionHeaderTableEntrySize();
    }

    public ElfProgramHeaderEntry getProgramHeaderTableEntry(int index) {
        int size = programEntries.size();
        ElfProgramHeaderEntry entry;
        if (index < size) {
            entry = programEntries.get(index);
            if (entry != null) {
                return entry;
            }
        } else {
            programEntries.ensureCapacity(index + 1);
            do {
                programEntries.add(null);
            } while (index > size++);
        }
        final long position = getProgramHeaderTableOffset() + index * getActualProgramHeaderTableEntrySize();
        entry = constructProgramHeaderTableEntry(position);
        programEntries.set(index, entry);
        return entry;
    }

    public ElfSectionHeaderEntry getSectionHeaderTableEntry(int index) {
        int size = sectionEntries.size();
        ElfSectionHeaderEntry entry;
        if (index < size) {
            entry = sectionEntries.get(index);
            if (entry != null) {
                return entry;
            }
        } else {
            sectionEntries.ensureCapacity(index + 1);
            do {
                sectionEntries.add(null);
            } while (index > size++);
        }
        final long position = getSectionHeaderTableOffset() + index * getActualSectionHeaderTableEntrySize();
        entry = constructSectionHeaderTableEntry(position);
        sectionEntries.set(index, entry);
        return entry;
    }

    public ElfSectionHeaderEntry getSectionHeaderTableEntry(String name) {
        ElfSectionHeaderEntry entry = sectionCache.get(name);
        if (entry != null) {
            return entry;
        }
        final int cnt = getSectionHeaderTableEntryCount();
        for (int i = 0; i < cnt; i++) {
            entry = getSectionHeaderTableEntry(i);
            if (name.equals(entry.getName())) {
                sectionCache.put(name, entry);
                return entry;
            }
        }
        // remove this bit if this becomes a leaky problem
        sectionCache.put(name, null);
        return null;
    }

    public ElfSectionHeaderEntry getSectionHeaderTableEntry(Elf.Section.Type type) {
        final int cnt = getSectionHeaderTableEntryCount();
        for (int i = 0; i < cnt; i++) {
            ElfSectionHeaderEntry entry = getSectionHeaderTableEntry(i);
            if (entry == null) {
                // not really possible but...
                return null;
            }
            if (type == entry.getType()) {
                return entry;
            }
        }
        return null;
    }

    public ElfSymbolTableEntry findSymbol(String symbolName) {
        // todo: support SHT_HASH
        ElfSymbolTableEntry entry = symbolCache.get(symbolName);
        if (entry != null) {
            return entry;
        }
        int idx = 0;
        for (;;) {
            entry = getSymbolTableEntry(idx++, false);
            if (entry == null) {
                return null;
            } else if (entry.nameEquals(symbolName)) {
                symbolCache.put(entry.getName(), entry);
                return entry;
            }
        }
    }

    public ElfSymbolTableEntry getSymbolTableEntry(int index, boolean dynamic) {
        final Elf.Section.Type.Std type = dynamic ? Elf.Section.Type.Std.DYN_SYM : Elf.Section.Type.Std.SYM_TAB;
        final ArrayList<ElfSymbolTableEntry> list = dynamic ? dynamicSymbols : staticSymbols;
        final ElfSectionHeaderEntry symtab = getSectionHeaderTableEntry(type);
        if (symtab == null) {
            return null;
        }
        final long relative = index * symtab.getFixedEntrySize();
        if (relative >= symtab.getSize()) {
            return null;
        }
        ElfSymbolTableEntry entry;
        int listSize = list.size();
        if (index < listSize) {
            entry = list.get(index);
            if (entry != null) {
                return entry;
            }
        } else {
            list.ensureCapacity(index + 1);
            do {
                list.add(null);
            } while (index > listSize++);
        }
        entry = constructSymbolTableEntry(symtab, symtab.getOffset() + relative);
        list.set(index, entry);
        return entry;
    }

    abstract ElfProgramHeaderEntry constructProgramHeaderTableEntry(long position);

    abstract ElfSectionHeaderEntry constructSectionHeaderTableEntry(long position);

    abstract ElfSymbolTableEntry constructSymbolTableEntry(ElfSectionHeaderEntry sectionHeader, long position);

    abstract ElfRelocationTableEntry constructRelocationTableEntry(ElfSectionHeaderEntry sectionHeader, long position);

    static final class _32 extends ElfHeader {
        _32(final BinaryBuffer backingBuffer) {
            super(backingBuffer, 0x24);
        }

        public Elf.Class getElfClass() {
            return Elf.Class.Std._32;
        }

        public int getExpectedSize() {
            return 52;
        }

        public int getActualSize() {
            return backingBuffer.getShortUnsigned(0x28);
        }

        public void setSize() {
            backingBuffer.putShort(0x28, 52);
        }

        public long getEntryPoint() {
            return backingBuffer.getIntUnsigned(0x18);
        }

        public void setEntryPoint(final long entryPoint) {
            backingBuffer.putInt(0x18, entryPoint);
        }

        public long getProgramHeaderTableOffset() {
            return backingBuffer.getIntUnsigned(0x1C);
        }

        public void setProgramHeaderTableOffset(final long offset) {
            backingBuffer.putInt(0x1C, offset);
        }

        public long getSectionHeaderTableOffset() {
            return backingBuffer.getIntUnsigned(0x20);
        }

        public void setSectionHeaderTableOffset(final long offset) {
            backingBuffer.putInt(0x20, offset);
        }

        public int getExpectedProgramHeaderTableEntrySize() {
            return 32;
        }

        public int getActualProgramHeaderTableEntrySize() {
            return backingBuffer.getShortUnsigned(0x2A);
        }

        public void setProgramHeaderTableEntrySize() {
            backingBuffer.putShort(0x2A, 32);
        }

        public int getProgramHeaderTableEntryCount() {
            return backingBuffer.getShortUnsigned(0x2C);
        }

        public void setProgramHeaderTableEntryCount(final int count) {
            backingBuffer.putShort(0x2C, count);
        }

        public void setSectionHeaderTableEntrySize() {
            backingBuffer.putShort(0x2E, 40);
        }

        public int getExpectedSectionHeaderTableEntrySize() {
            return 40;
        }

        public int getActualSectionHeaderTableEntrySize() {
            return backingBuffer.getShortUnsigned(0x2E);
        }

        public int getSectionHeaderTableEntryCount() {
            return backingBuffer.getShortUnsigned(0x30);
        }

        public void setSectionHeaderTableEntryCount(final int count) {
            backingBuffer.putShort(0x30, count);
        }

        public int getStringTableSectionHeaderIndex() {
            return getBackingBuffer().getShortUnsigned(0x32);
        }

        public void setStringTableSectionHeaderIndex(final int index) {
            getBackingBuffer().putShort(0x32, index);
        }

        ElfProgramHeaderEntry constructProgramHeaderTableEntry(final long position) {
            return new ElfProgramHeaderEntry._32(backingBuffer, position);
        }

        ElfSectionHeaderEntry constructSectionHeaderTableEntry(final long position) {
            return new ElfSectionHeaderEntry._32(this, position);
        }

        ElfSymbolTableEntry constructSymbolTableEntry(final ElfSectionHeaderEntry sectionHeader, final long position) {
            return new ElfSymbolTableEntry._32(sectionHeader, position);
        }

        ElfRelocationTableEntry constructRelocationTableEntry(final ElfSectionHeaderEntry sectionHeader, final long position) {
            return new ElfRelocationTableEntry._32(sectionHeader, position);
        }
    }

    static final class _64 extends ElfHeader {
        _64(final BinaryBuffer backingBuffer) {
            super(backingBuffer, 0x30);
        }

        public Elf.Class getElfClass() {
            return Elf.Class.Std._64;
        }

        public int getExpectedSize() {
            return 64;
        }

        public int getActualSize() {
            return backingBuffer.getShortUnsigned(0x34);
        }

        public void setSize() {
            backingBuffer.putShort(0x34, 64);
        }

        public long getEntryPoint() {
            return backingBuffer.getLong(0x18);
        }

        public void setEntryPoint(final long entryPoint) {
            backingBuffer.putLong(0x18, entryPoint);
        }

        public long getProgramHeaderTableOffset() {
            return backingBuffer.getLong(0x20);
        }

        public void setProgramHeaderTableOffset(final long offset) {
            backingBuffer.putLong(0x20, offset);
        }

        public long getSectionHeaderTableOffset() {
            return backingBuffer.getLong(0x28);
        }

        public void setSectionHeaderTableOffset(final long offset) {
            backingBuffer.putLong(0x28, offset);
        }

        public int getExpectedProgramHeaderTableEntrySize() {
            return 56;
        }

        public int getActualProgramHeaderTableEntrySize() {
            return backingBuffer.getShortUnsigned(0x36);
        }

        public void setProgramHeaderTableEntrySize() {
            backingBuffer.putShort(0x36, 56);
        }

        public int getProgramHeaderTableEntryCount() {
            return backingBuffer.getShortUnsigned(0x38);
        }

        public void setProgramHeaderTableEntryCount(final int count) {
            backingBuffer.putShort(0x38, count);
        }

        public void setSectionHeaderTableEntrySize() {
            backingBuffer.putShort(0x3A, 64);
        }

        public int getExpectedSectionHeaderTableEntrySize() {
            return 64;
        }

        public int getActualSectionHeaderTableEntrySize() {
            return backingBuffer.getShortUnsigned(0x3A);
        }

        public int getSectionHeaderTableEntryCount() {
            return backingBuffer.getShortUnsigned(0x3C);
        }

        public void setSectionHeaderTableEntryCount(final int count) {
            backingBuffer.putShort(0x3C, count);
        }

        public int getStringTableSectionHeaderIndex() {
            return getBackingBuffer().getShortUnsigned(0x3E);
        }

        public void setStringTableSectionHeaderIndex(final int index) {
            getBackingBuffer().putShort(0x3E, index);
        }

        ElfProgramHeaderEntry constructProgramHeaderTableEntry(final long position) {
            return new ElfProgramHeaderEntry._64(backingBuffer, position);
        }

        ElfSectionHeaderEntry constructSectionHeaderTableEntry(final long position) {
            return new ElfSectionHeaderEntry._64(this, position);
        }

        ElfSymbolTableEntry constructSymbolTableEntry(final ElfSectionHeaderEntry sectionHeader, final long position) {
            return new ElfSymbolTableEntry._64(sectionHeader, position);
        }

        ElfRelocationTableEntry constructRelocationTableEntry(final ElfSectionHeaderEntry sectionHeader, final long position) {
            return new ElfRelocationTableEntry._64(sectionHeader, position);
        }
    }
}
