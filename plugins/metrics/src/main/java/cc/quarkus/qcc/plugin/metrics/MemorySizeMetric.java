package org.qbicc.plugin.metrics;

public final class MemorySizeMetric extends Metric<MemorySizeMetric> {

    MemorySizeMetric(String name, MemorySizeMetric parent) {
        super(name, parent);
    }

    @Override
    MemorySizeMetric constructChild(String name) {
        return new MemorySizeMetric(name, this);
    }

    public void add(long amount) {
        addRawValue(amount);
    }

    public void add(long amount, Scale scale) {
        addRawValue(amount << scale.getBitShift());
    }

    @Override
    String getDescription() {
        return "Size";
    }

    @Override
    public StringBuilder getFormattedValue(StringBuilder target) {
        long value = getRawValue();
        int ntz = Long.numberOfTrailingZeros(Long.highestOneBit(value));
        Scale scale = value == 0 ? Scale.BYTES : Scale.ofBits(ntz + 4);
        long scaled = value >>> scale.getBitShift();
        int frac = (int) (((value >>> scale.prev().getBitShift()) & 0x3FF) * 1000 / 1000);
        target.append(Long.toUnsignedString(scaled));
        if (frac >= 100) {
            if (frac % 100 == 0) {
                frac /= 100;
            } else if (frac % 10 == 0) {
                frac /= 10;
            }
            target.append('.').append(frac);
        } else if (frac >= 10) {
            if (frac % 10 == 0) {
                frac /= 10;
            }
            target.append('.').append('0').append(frac);
        } else if (frac >= 1) {
            target.append('.').append('0').append('0').append(frac);
        }
        return target.append(scale.getSuffix());
    }

    public enum Scale {
        BYTES(0, 'B'),
        KILO(10, 'K'),
        MEGA(20, 'M'),
        GIGA(30, 'G'),
        TERA(40, 'T'),
        PETA(50, 'P'),
        EXA(60, 'E'),
        ;

        static final Scale[] SCALES = Scale.values();

        public static final Scale MIN = SCALES[0];
        public static final Scale MAX = SCALES[SCALES.length - 1];

        private final int bitShift;
        private final char suffix;

        Scale(final int bitShift, final char suffix) {
            this.bitShift = bitShift;
            this.suffix = suffix;
        }

        public int getBitShift() {
            return bitShift;
        }

        public char getSuffix() {
            return suffix;
        }

        public Scale next() {
            return this == MAX ? this : SCALES[ordinal() + 1];
        }

        public Scale prev() {
            return this == MIN ? this : SCALES[ordinal() - 1];
        }

        public static Scale min(Scale a, Scale b) {
            return a.compareTo(b) < 0 ? a : b;
        }

        public static Scale max(Scale a, Scale b) {
            return a.compareTo(b) > 0 ? a : b;
        }

        public static Scale ofUnsigned(long unsigned) {
            int ntz = Long.numberOfTrailingZeros(Long.highestOneBit(unsigned));
            return ofBits(ntz);
        }

        public static Scale ofBits(int bitCount) {
            int sc = bitCount / 10;
            if (sc < 0) return MIN;
            if (sc >= SCALES.length) return MAX;
            return SCALES[sc];
        }

        public static Scale of(String suffix) {
            if (suffix.length() == 1 || suffix.length() == 2 && Character.toUpperCase(suffix.charAt(1)) == 'B') {
                return of(suffix.charAt(0));
            } else {
                throw new IllegalArgumentException("Invalid suffix: " + suffix);
            }
        }

        public static Scale of(char suffixChar) {
            suffixChar = Character.toUpperCase(suffixChar);
            switch (suffixChar) {
                case 'B': return BYTES;
                case 'K': return KILO;
                case 'M': return MEGA;
                case 'G': return GIGA;
                case 'T': return TERA;
                case 'P': return PETA;
                case 'E': return EXA;
                default: throw new IllegalArgumentException("Invalid suffix: " + suffixChar);
            }
        }
    }
}
