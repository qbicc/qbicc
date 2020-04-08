package cc.quarkus.qcc.machine.file.elf;

import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class ElfProgramHeaderEntry {
    final BinaryBuffer backingBuffer;
    final long position;
    final MappedBitSet<Elf.Program.Flag> flags;

    ElfProgramHeaderEntry(final BinaryBuffer backingBuffer, final long position, final MappedBitSet<Elf.Program.Flag> flags) {
        this.backingBuffer = backingBuffer;
        this.position = position;
        this.flags = flags;
    }

    public BinaryBuffer getBackingBuffer() {
        return backingBuffer;
    }

    public long getPosition() {
        return position;
    }

    public Elf.Program.Type getType() {
        return Elf.Program.Type.forValue(backingBuffer.getInt(position));
    }

    public void setType(Elf.Program.Type type) {
        backingBuffer.putInt(position, type.getValue());
    }

    public MappedBitSet<Elf.Program.Flag> getFlags() {
        return flags;
    }

    public abstract long getOffset();

    public abstract void setOffset(long offset);

    public abstract long getVirtualAddress();

    public abstract void setVirtualAddress(long vAddr);

    public abstract long getPhysicalAddress();

    public abstract void setPhysicalAddress(long pAddr);

    public abstract long getFileSize();

    public abstract void setFileSize(long size);

    public abstract long getMemorySize();

    public abstract void setMemorySize(long size);

    public abstract long getAlignment();

    public abstract int getEntrySize();

    public void setAlignment(long alignment) {
        if (alignment != 0) {
            if (Long.bitCount(alignment) != 1) {
                throw new IllegalArgumentException("Alignment value must be zero or have exactly one bit set");
            }
        }
        setAlignmentUnchecked(alignment);
    }

    abstract void setAlignmentUnchecked(long alignment);

    static final class _32 extends ElfProgramHeaderEntry {

        _32(final BinaryBuffer backingBuffer, final long position) {
            super(backingBuffer, position,
                    MappedBitSet.map32Bits(backingBuffer, position + 24, Elf.Program.Flag.class, Elf.Program.Flag::forValue));
        }

        public long getOffset() {
            return backingBuffer.getIntUnsigned(position + 4);
        }

        public void setOffset(long offset) {
            backingBuffer.putInt(position + 4, offset);
        }

        public long getVirtualAddress() {
            return backingBuffer.getIntUnsigned(position + 8);
        }

        public void setVirtualAddress(long vAddr) {
            backingBuffer.putInt(position + 8, vAddr);
        }

        public long getPhysicalAddress() {
            return backingBuffer.getIntUnsigned(position + 12);
        }

        public void setPhysicalAddress(long pAddr) {
            backingBuffer.putInt(position + 12, pAddr);
        }

        public long getFileSize() {
            return backingBuffer.getIntUnsigned(position + 16);
        }

        public void setFileSize(long size) {
            backingBuffer.putInt(position + 16, size);
        }

        public long getMemorySize() {
            return backingBuffer.getIntUnsigned(position + 20);
        }

        public void setMemorySize(long size) {
            backingBuffer.putInt(position + 20, size);
        }

        public long getAlignment() {
            return backingBuffer.getIntUnsigned(position + 28);
        }

        void setAlignmentUnchecked(long alignment) {
            backingBuffer.putInt(position + 28, alignment);
        }

        public int getEntrySize() {
            return 32;
        }
    }

    static final class _64 extends ElfProgramHeaderEntry {

        _64(final BinaryBuffer backingBuffer, final long position) {
            super(backingBuffer, position,
                    MappedBitSet.map32Bits(backingBuffer, position + 4, Elf.Program.Flag.class, Elf.Program.Flag::forValue));
        }

        public long getOffset() {
            return backingBuffer.getLong(position + 8);
        }

        public void setOffset(long offset) {
            backingBuffer.putLong(position + 8, offset);
        }

        public long getVirtualAddress() {
            return backingBuffer.getLong(position + 16);
        }

        public void setVirtualAddress(long vAddr) {
            backingBuffer.putLong(position + 16, vAddr);
        }

        public long getPhysicalAddress() {
            return backingBuffer.getLong(position + 24);
        }

        public void setPhysicalAddress(long pAddr) {
            backingBuffer.putLong(position + 24, pAddr);
        }

        public long getFileSize() {
            return backingBuffer.getLong(position + 32);
        }

        public void setFileSize(long size) {
            backingBuffer.putLong(position + 32, size);
        }

        public long getMemorySize() {
            return backingBuffer.getLong(position + 40);
        }

        public void setMemorySize(long size) {
            backingBuffer.putLong(position + 40, size);
        }

        public long getAlignment() {
            return backingBuffer.getLong(position + 48);
        }

        void setAlignmentUnchecked(long alignment) {
            backingBuffer.putLong(position + 48, alignment);
        }

        public int getEntrySize() {
            return 56;
        }
    }
}
