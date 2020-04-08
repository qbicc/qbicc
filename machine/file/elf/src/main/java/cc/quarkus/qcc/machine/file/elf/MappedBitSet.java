package cc.quarkus.qcc.machine.file.elf;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;

import cc.quarkus.qcc.machine.file.bin.BinaryBuffer;

/**
 *
 */
public abstract class MappedBitSet<E extends Elf.NumericEnumeration> extends AbstractSet<E> {
    final BinaryBuffer backingBuffer;
    final long position;
    private final IntFunction<E> decoder;
    private final Class<E> type;
    final LongUnaryOperator mask;

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map8Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder, LongUnaryOperator mask) {
        return new _8<>(backingBuffer, position, decoder, type, mask);
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map8Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder) {
        return map8Bits(backingBuffer, position, type, decoder, LongUnaryOperator.identity());
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map16Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder, LongUnaryOperator mask) {
        return new _16<>(backingBuffer, position, decoder, type, mask);
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map16Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder) {
        return map16Bits(backingBuffer, position, type, decoder, LongUnaryOperator.identity());
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map32Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder, LongUnaryOperator mask) {
        return new _32<>(backingBuffer, position, decoder, type, mask);
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map32Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder) {
        return new _32<>(backingBuffer, position, decoder, type, LongUnaryOperator.identity());
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map64Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder, LongUnaryOperator mask) {
        return new _64<>(backingBuffer, position, decoder, type, mask);
    }

    public static <E extends Elf.NumericEnumeration> MappedBitSet<E> map64Bits(BinaryBuffer backingBuffer, long position,
            Class<E> type, IntFunction<E> decoder) {
        return new _64<>(backingBuffer, position, decoder, type, LongUnaryOperator.identity());
    }

    MappedBitSet(final BinaryBuffer backingBuffer, final long position, final IntFunction<E> decoder, final Class<E> type,
            LongUnaryOperator mask) {
        this.backingBuffer = backingBuffer;
        this.position = position;
        this.decoder = decoder;
        this.type = type;
        this.mask = mask;
    }

    final long getValue() {
        return mask.applyAsLong(getRawValue());
    }

    public abstract long getRawValue();

    final void setValue(long value) {
        setRawValue(mask.applyAsLong(value));
    }

    public abstract void setRawValue(long value);

    public Iterator<E> iterator() {
        return new Iterator<E>() {
            long v = getValue();

            public boolean hasNext() {
                return v != 0;
            }

            public E next() {
                final long lob = Long.lowestOneBit(v);
                if (lob == 0) {
                    throw new NoSuchElementException();
                }
                v ^= lob;
                return decoder.apply(Long.numberOfTrailingZeros(lob));
            }
        };
    }

    public int size() {
        return Long.bitCount(getValue());
    }

    public boolean isEmpty() {
        return getValue() == 0;
    }

    public void forEach(final Consumer<? super E> action) {
        long v = getValue();
        while (v != 0) {
            final long lob = Long.lowestOneBit(v);
            action.accept(decoder.apply(Long.numberOfTrailingZeros(lob)));
            v ^= lob;
        }
    }

    public boolean removeIf(final Predicate<? super E> filter) {
        long orig = getValue();
        long v = orig;
        long remove = 0;
        while (v != 0) {
            final long lob = Long.lowestOneBit(v);
            if (filter.test(decoder.apply(Long.numberOfTrailingZeros(lob)))) {
                remove |= lob;
            }
            v ^= lob;
        }
        if (remove == 0) {
            return false;
        }
        setValue(orig & ~remove);
        return true;
    }

    public boolean add(final E e) {
        type.cast(e);
        final int val = e.getValue();
        final long value = getValue();
        final long bit = 1L << val;
        if ((value & bit) == 1) {
            // already set
            return false;
        }
        setValue(value | bit);
        return true;
    }

    public boolean remove(final Object o) {
        if (type.isInstance(o)) {
            final E ne = type.cast(o);
            final int val = ne.getValue();
            final long value = getValue();
            final long bit = 1L << val;
            if ((value & bit) == 0) {
                // already clear
                return false;
            }
            setValue(value & ~bit);
            return true;
        } else {
            return false;
        }
    }

    public void clear() {
        setValue(0);
    }

    public boolean contains(final Object o) {
        if (type.isInstance(o)) {
            final E ne = type.cast(o);
            final int val = ne.getValue();
            final long value = getValue();
            final long bit = 1L << val;
            return (value & bit) != 0;
        } else {
            return false;
        }
    }

    public boolean removeAll(final Collection<?> c) {
        long remove = 0;
        if (c instanceof MappedBitSet && ((MappedBitSet<?>) c).type == type) {
            remove = ((MappedBitSet<?>) c).getValue();
        } else {
            for (Object o : c) {
                if (type.isInstance(o)) {
                    remove |= 1L << type.cast(o).getValue();
                }
            }
        }
        if (remove == 0) {
            return false;
        }
        final long old = getValue();
        if ((old & remove) == 0) {
            return false;
        }
        setValue(old & ~remove);
        return true;
    }

    public boolean addAll(final Collection<? extends E> c) {
        long add = 0;
        if (c instanceof MappedBitSet && ((MappedBitSet<?>) c).type == type) {
            add = ((MappedBitSet<?>) c).getValue();
        } else {
            for (E e : c) {
                add |= 1L << e.getValue();
            }
        }
        if (add == 0) {
            return false;
        }
        final long old = getValue();
        if ((old & add) == add) {
            return false;
        }
        setValue(old | add);
        return true;
    }

    public boolean retainAll(final Collection<?> c) {
        long retain = 0;
        if (c instanceof MappedBitSet && ((MappedBitSet<?>) c).type == type) {
            retain = ((MappedBitSet<?>) c).getValue();
        } else {
            for (Object o : c) {
                if (type.isInstance(o)) {
                    retain |= 1L << type.cast(o).getValue();
                }
            }
        }
        final long old = getValue();
        if ((old & ~retain) == 0) {
            return false;
        }
        setValue(old & retain);
        return true;
    }

    public boolean containsAll(final Collection<?> c) {
        long find = 0;
        if (c instanceof MappedBitSet && ((MappedBitSet<?>) c).type == type) {
            find = ((MappedBitSet<?>) c).getValue();
        } else {
            for (Object o : c) {
                if (!type.isInstance(o)) {
                    return false;
                }
                find |= 1L << type.cast(o).getValue();
            }
        }
        return (getValue() & find) == find;
    }

    static final class _8<E extends Elf.NumericEnumeration> extends MappedBitSet<E> {
        _8(final BinaryBuffer backingBuffer, final long position, final IntFunction<E> decoder, final Class<E> type,
                final LongUnaryOperator mask) {
            super(backingBuffer, position, decoder, type, mask);
        }

        public long getRawValue() {
            return backingBuffer.getByteUnsigned(position);
        }

        public void setRawValue(final long value) {
            backingBuffer.putByte(position, value);
        }
    }

    static final class _16<E extends Elf.NumericEnumeration> extends MappedBitSet<E> {
        _16(final BinaryBuffer backingBuffer, final long position, final IntFunction<E> decoder, final Class<E> type,
                final LongUnaryOperator mask) {
            super(backingBuffer, position, decoder, type, mask);
        }

        public long getRawValue() {
            return backingBuffer.getShortUnsigned(position);
        }

        public void setRawValue(final long value) {
            backingBuffer.putShort(position, value);
        }
    }

    static final class _32<E extends Elf.NumericEnumeration> extends MappedBitSet<E> {
        _32(final BinaryBuffer backingBuffer, final long position, final IntFunction<E> decoder, final Class<E> type,
                final LongUnaryOperator mask) {
            super(backingBuffer, position, decoder, type, mask);
        }

        public long getRawValue() {
            return backingBuffer.getIntUnsigned(position);
        }

        public void setRawValue(final long value) {
            backingBuffer.putInt(position, value);
        }
    }

    static final class _64<E extends Elf.NumericEnumeration> extends MappedBitSet<E> {
        _64(final BinaryBuffer backingBuffer, final long position, final IntFunction<E> decoder, final Class<E> type,
                final LongUnaryOperator mask) {
            super(backingBuffer, position, decoder, type, mask);
        }

        public long getRawValue() {
            return backingBuffer.getLong(position);
        }

        public void setRawValue(final long value) {
            backingBuffer.putLong(position, value);
        }
    }
}
