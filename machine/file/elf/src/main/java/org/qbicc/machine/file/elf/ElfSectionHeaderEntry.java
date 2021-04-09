package org.qbicc.machine.file.elf;

import java.util.Set;

import org.qbicc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class ElfSectionHeaderEntry {
    final ElfHeader elfHeader;
    final long position;
    final Set<Elf.Section.Flag> sectionFlags;
    String name = null;

    ElfSectionHeaderEntry(final ElfHeader elfHeader, final long position, final Set<Elf.Section.Flag> sectionFlags) {
        this.elfHeader = elfHeader;
        this.position = position;
        this.sectionFlags = sectionFlags;
    }

    public BinaryBuffer getBackingBuffer() {
        return elfHeader.getBackingBuffer();
    }

    public ElfHeader getElfHeader() {
        return elfHeader;
    }

    public long getPosition() {
        return position;
    }

    public int getNameIndex() {
        return getBackingBuffer().getInt(position);
    }

    public void setNameIndex(int index) {
        getBackingBuffer().putInt(position, index);
    }

    public void setNameIndex(long index) {
        getBackingBuffer().putInt(position, index);
    }

    public String getName() {
        String name = this.name;
        if (name != null) {
            return name;
        }
        final int nameIndex = getNameIndex();
        if (nameIndex == 0) {
            return null;
        }
        return this.name = elfHeader.getString(nameIndex);
    }

    public Elf.Section.Type getType() {
        return getElfHeader().getMachine().decodeSectionType(getBackingBuffer().getInt(position + 4));
    }

    public void setType(Elf.Section.Type type) {
        getBackingBuffer().putInt(position + 4, type.getValue());
    }

    public Set<Elf.Section.Flag> getFlags() {
        return sectionFlags;
    }

    public void initialize() {
        setAlignment(1);
    }

    public abstract long getAddress();

    public abstract void setAddress(long address);

    public abstract long getOffset();

    public abstract void setOffset(long offset);

    public abstract long getSize();

    public abstract void setSize(long size);

    public abstract int getLinkedSectionIndex();

    public abstract void setLinkedSectionIndex(int index);

    public abstract int getSectionInfo();

    public abstract void setSectionInfo(int info);

    public abstract long getAlignment();

    public abstract void setAlignment(long alignment);

    public abstract long getFixedEntrySize();

    public abstract void setFixedEntrySize(long size);

    public abstract int getExpectedSize();

    static class _32 extends ElfSectionHeaderEntry {
        _32(final ElfHeader elf, final long position) {
            super(elf, position, MappedBitSet.map64Bits(elf.getBackingBuffer(), position + 8, Elf.Section.Flag.class,
                    Elf.Section.Flag::forValue));
        }

        public long getAddress() {
            return getBackingBuffer().getIntUnsigned(getPosition() + 0x0C);
        }

        public void setAddress(final long address) {
            getBackingBuffer().putInt(getPosition() + 0x0C, address);
        }

        public long getOffset() {
            return getBackingBuffer().getIntUnsigned(getPosition() + 0x10);
        }

        public void setOffset(final long offset) {
            getBackingBuffer().putInt(getPosition() + 0x10, offset);
        }

        public long getSize() {
            return getBackingBuffer().getIntUnsigned(getPosition() + 0x14);
        }

        public void setSize(final long size) {
            getBackingBuffer().putInt(getPosition() + 0x14, size);
        }

        public int getLinkedSectionIndex() {
            return getBackingBuffer().getInt(getPosition() + 0x18);
        }

        public void setLinkedSectionIndex(final int index) {
            getBackingBuffer().putInt(getPosition() + 0x18, index);
        }

        public int getSectionInfo() {
            return getBackingBuffer().getInt(getPosition() + 0x1C);
        }

        public void setSectionInfo(final int info) {
            getBackingBuffer().putInt(getPosition() + 0x1C, info);
        }

        public long getAlignment() {
            return getBackingBuffer().getIntUnsigned(getPosition() + 0x20);
        }

        public void setAlignment(final long alignment) {
            getBackingBuffer().putInt(getPosition() + 0x20, alignment);
        }

        public long getFixedEntrySize() {
            return getBackingBuffer().getIntUnsigned(getPosition() + 0x24);
        }

        public void setFixedEntrySize(final long size) {
            getBackingBuffer().putInt(getPosition() + 0x24, size);
        }

        public int getExpectedSize() {
            return 0x28;
        }
    }

    public static class _64 extends ElfSectionHeaderEntry {
        _64(final ElfHeader elf, final long position) {
            super(elf, position, MappedBitSet.map64Bits(elf.getBackingBuffer(), position + 8, Elf.Section.Flag.class,
                    Elf.Section.Flag::forValue));
        }

        public long getAddress() {
            return getBackingBuffer().getLong(getPosition() + 0x10);
        }

        public void setAddress(final long address) {
            getBackingBuffer().putLong(getPosition() + 0x10, address);
        }

        public long getOffset() {
            return getBackingBuffer().getLong(getPosition() + 0x18);
        }

        public void setOffset(final long offset) {
            getBackingBuffer().putLong(getPosition() + 0x18, offset);
        }

        public long getSize() {
            return getBackingBuffer().getLong(getPosition() + 0x20);
        }

        public void setSize(final long size) {
            getBackingBuffer().putLong(getPosition() + 0x20, size);
        }

        public int getLinkedSectionIndex() {
            return getBackingBuffer().getInt(getPosition() + 0x28);
        }

        public void setLinkedSectionIndex(final int index) {
            getBackingBuffer().putInt(getPosition() + 0x28, index);
        }

        public int getSectionInfo() {
            return getBackingBuffer().getInt(getPosition() + 0x2C);
        }

        public void setSectionInfo(final int info) {
            getBackingBuffer().putInt(getPosition() + 0x2C, info);
        }

        public long getAlignment() {
            return getBackingBuffer().getLong(getPosition() + 0x30);
        }

        public void setAlignment(final long alignment) {
            getBackingBuffer().putLong(getPosition() + 0x30, alignment);
        }

        public long getFixedEntrySize() {
            return getBackingBuffer().getLong(getPosition() + 0x38);
        }

        public void setFixedEntrySize(final long size) {
            getBackingBuffer().putLong(getPosition() + 0x38, size);
        }

        public int getExpectedSize() {
            return 0x40;
        }
    }
}
