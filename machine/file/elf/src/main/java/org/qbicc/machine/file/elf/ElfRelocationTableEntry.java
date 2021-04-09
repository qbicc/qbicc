package org.qbicc.machine.file.elf;

import org.qbicc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class ElfRelocationTableEntry {
    final ElfSectionHeaderEntry elfSectionHeaderEntry;
    final long position;

    public ElfRelocationTableEntry(final ElfSectionHeaderEntry elfSectionHeaderEntry, final long position) {
        this.elfSectionHeaderEntry = elfSectionHeaderEntry;
        this.position = position;
    }

    public ElfSectionHeaderEntry getElfSectionHeaderEntry() {
        return elfSectionHeaderEntry;
    }

    public ElfHeader getElfHeader() {
        return getElfSectionHeaderEntry().getElfHeader();
    }

    public BinaryBuffer getBackingBuffer() {
        return getElfHeader().getBackingBuffer();
    }

    public long getPosition() {
        return position;
    }

    public abstract long getOffset();

    public abstract void setOffset(long offset);

    public abstract int getSymbolIndex();

    public abstract void setSymbolIndex(int index);

    abstract int getRawType();

    abstract void setRawType(int rawType);

    public abstract long getAddend();

    public abstract void setAddend(long addend);

    public Elf.Relocation.Type getType() {
        return getElfHeader().getMachine().decodeRelocationType(getRawType());
    }

    public void setType(Elf.Relocation.Type type) {
        setRawType(type.getValue());
    }

    void checkAddendAccess() {
        final Elf.Section.Type sectionType = getElfSectionHeaderEntry().getType();
        if (sectionType != Elf.Section.Type.Std.REL_A) {
            throw new IllegalArgumentException("Cannot access addend for section type of " + sectionType);
        }
    }

    static final class _32 extends ElfRelocationTableEntry {
        _32(final ElfSectionHeaderEntry elfSectionHeaderEntry, final long position) {
            super(elfSectionHeaderEntry, position);
        }

        public long getOffset() {
            return getBackingBuffer().getIntUnsigned(getPosition());
        }

        public void setOffset(final long offset) {
            getBackingBuffer().putInt(getPosition(), offset);
        }

        public int getSymbolIndex() {
            return (int) (getBackingBuffer().getIntUnsigned(getPosition() + 0x04) >>> 8);
        }

        public void setSymbolIndex(final int index) {
            final BinaryBuffer buf = getBackingBuffer();
            final long idx = getPosition() + 0x04;
            buf.putInt(idx, buf.getIntUnsigned(idx) & 0xff | index << 8);
        }

        int getRawType() {
            return getBackingBuffer().getInt(getPosition() + 0x04) & 0xff;
        }

        void setRawType(final int rawType) {
            final BinaryBuffer buf = getBackingBuffer();
            final long idx = getPosition() + 0x04;
            buf.putInt(idx, buf.getIntUnsigned(idx) & 0xffffff00 | rawType & 0xff);
        }

        public long getAddend() {
            checkAddendAccess();
            return getBackingBuffer().getInt(getPosition() + 0x08);
        }

        public void setAddend(final long addend) {
            checkAddendAccess();
            getBackingBuffer().putInt(getPosition() + 0x08, addend);
        }
    }

    static final class _64 extends ElfRelocationTableEntry {
        _64(final ElfSectionHeaderEntry elfSectionHeaderEntry, final long position) {
            super(elfSectionHeaderEntry, position);
        }

        public long getOffset() {
            return getBackingBuffer().getLong(getPosition());
        }

        public void setOffset(final long offset) {
            getBackingBuffer().putLong(getPosition(), offset);
        }

        public int getSymbolIndex() {
            return (int) (getBackingBuffer().getLong(getPosition() + 0x08) >>> 32);
        }

        public void setSymbolIndex(final int index) {
            final BinaryBuffer buf = getBackingBuffer();
            final long idx = getPosition() + 0x08;
            buf.putLong(idx, buf.getLong(idx) & 0xffffffffL | ((long)index) << 32);
        }

        int getRawType() {
            return (int) getBackingBuffer().getLong(getPosition() + 0x08);
        }

        void setRawType(final int rawType) {
            // access as two separate words because the order is endian-dependent
            final BinaryBuffer buf = getBackingBuffer();
            final long idx = getPosition() + 0x08;
            buf.putLong(idx, buf.getLong(idx) & 0xffffffff_00000000L | rawType & 0xffffffffL);
        }

        public long getAddend() {
            checkAddendAccess();
            return getBackingBuffer().getLong(getPosition() + 0x10);
        }

        public void setAddend(final long addend) {
            checkAddendAccess();
            getBackingBuffer().putLong(getPosition() + 0x10, addend);
        }
    }
}
