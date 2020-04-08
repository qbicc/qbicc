package cc.quarkus.qcc.machine.file.elf;

import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class ElfSymbolTableEntry {
    final ElfSectionHeaderEntry sectionHeaderEntry;
    final long position;
    String name;

    ElfSymbolTableEntry(final ElfSectionHeaderEntry sectionHeaderEntry, final long position) {
        this.sectionHeaderEntry = sectionHeaderEntry;
        this.position = position;
    }

    public ElfSectionHeaderEntry getSectionHeaderEntry() {
        return sectionHeaderEntry;
    }

    public ElfHeader getElfHeader() {
        return getSectionHeaderEntry().getElfHeader();
    }

    public BinaryBuffer getBackingBuffer() {
        return sectionHeaderEntry.getBackingBuffer();
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

    public String getName() {
        String name = this.name;
        if (name == null) {
            final int stringSection = getSectionHeaderEntry().getLinkedSectionIndex();
            this.name = name = getElfHeader().getStringFromSection(stringSection, getNameIndex());
        }
        return name;
    }

    public boolean nameEquals(final String symbolName) {
        final int stringSection = getSectionHeaderEntry().getLinkedSectionIndex();
        return getElfHeader().stringEquals(stringSection, getNameIndex(), symbolName);
    }

    public abstract long getValueSize();

    public abstract void setValueSize(long size);

    public abstract long getValue();

    public abstract void setValue(long value);

    abstract int getRawInfo();

    abstract void setRawInfo(int value);

    public Elf.Symbol.Binding getBinding() {
        return Elf.Symbol.Binding.forValue(getRawInfo() >>> 4);
    }

    public void setBinding(Elf.Symbol.Binding binding) {
        setRawInfo(getRawInfo() & 0xF | binding.getValue() << 4);
    }

    public Elf.Symbol.Type getType() {
        return Elf.Symbol.Type.forValue(getRawInfo() & 0xF);
    }

    public void setType(Elf.Symbol.Type type) {
        setRawInfo(getRawInfo() & 0xfffffff0 | type.getValue() & 0xf);
    }

    public void setInfo(Elf.Symbol.Binding binding, Elf.Symbol.Type type) {
        setRawInfo(binding.getValue() << 4 | type.getValue());
    }

    abstract int getRawOther();

    abstract void setRawOther(int value);

    public Elf.Symbol.Visibility getVisibility() {
        return Elf.Symbol.Visibility.forValue(getRawOther() & 0x03);
    }

    public void setOther(Elf.Symbol.Visibility visibility) {
        setRawOther(visibility.getValue() & 0x03);
    }

    public abstract int getLinkedSectionIndex();

    public abstract void setLinkedSectionIndex(int index);

    static final class _32 extends ElfSymbolTableEntry {
        _32(final ElfSectionHeaderEntry sectionHeaderEntry, final long position) {
            super(sectionHeaderEntry, position);
        }

        public long getValueSize() {
            return getBackingBuffer().getIntUnsigned(position + 8);
        }

        public void setValueSize(final long size) {
            getBackingBuffer().putInt(position + 8, size);
        }

        public long getValue() {
            return getBackingBuffer().getIntUnsigned(position + 4);
        }

        public void setValue(final long value) {
            getBackingBuffer().putInt(position + 4, value);
        }

        int getRawInfo() {
            return getBackingBuffer().getByteUnsigned(position + 12);
        }

        void setRawInfo(final int value) {
            getBackingBuffer().putByte(position + 12, value);
        }

        int getRawOther() {
            return getBackingBuffer().getByteUnsigned(position + 13);
        }

        void setRawOther(final int value) {
            getBackingBuffer().putByte(position + 13, value);
        }

        public int getLinkedSectionIndex() {
            return getBackingBuffer().getShortUnsigned(position + 14);
        }

        public void setLinkedSectionIndex(final int index) {
            getBackingBuffer().putShort(position + 14, index);
        }
    }

    static final class _64 extends ElfSymbolTableEntry {
        _64(final ElfSectionHeaderEntry sectionHeaderEntry, final long position) {
            super(sectionHeaderEntry, position);
        }

        public long getValueSize() {
            return getBackingBuffer().getLong(position + 16);
        }

        public void setValueSize(final long size) {
            getBackingBuffer().putLong(position + 16, size);
        }

        public long getValue() {
            return getBackingBuffer().getLong(position + 8);
        }

        public void setValue(final long value) {
            getBackingBuffer().putLong(position + 8, value);
        }

        int getRawInfo() {
            return getBackingBuffer().getByteUnsigned(position + 4);
        }

        void setRawInfo(final int value) {
            getBackingBuffer().putByte(position + 4, value);
        }

        int getRawOther() {
            return getBackingBuffer().getByteUnsigned(position + 5);
        }

        void setRawOther(final int value) {
            getBackingBuffer().putByte(position + 5, value);
        }

        public int getLinkedSectionIndex() {
            return getBackingBuffer().getShortUnsigned(position + 6);
        }

        public void setLinkedSectionIndex(final int index) {
            getBackingBuffer().putShort(position + 6, index);
        }
    }
}
