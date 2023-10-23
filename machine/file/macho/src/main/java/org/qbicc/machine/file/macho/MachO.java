package org.qbicc.machine.file.macho;

import org.qbicc.machine.arch.Cpu;

/**
 *
 */
public final class MachO {
    private MachO() {}

    public static final int MH_MAGIC_32 = 0xfeedface;
    public static final int MH_CIGAM_32 = Integer.reverseBytes(MH_MAGIC_32);
    public static final int MH_MAGIC_64 = 0xfeedfacf;
    public static final int MH_CIGAM_64 = Integer.reverseBytes(MH_MAGIC_64);

    public enum FileType implements NumericEnumeration {
        MH_UNKNOWN(-1),
        MH_OBJECT(1),
        MH_EXECUTE(2),
        MH_FVMLIB(3),
        MH_CORE(4),
        MH_PRELOAD(5),
        MH_DYLIB(6),
        MH_DYLINKER(7),
        MH_BUNDLE(8),
        MH_DYLIB_STUB(9),
        MH_DSYM(0x0a),
        MH_KEXT_BUNDLE(0x0b),
        ;

        private final int value;

        FileType(final int value) {
            this.value = value;
        }

        private static final FileType[] VALUES = values();

        public static FileType forValue(int value) {
            return binarySearch(value, VALUES, MH_UNKNOWN);
        }

        public int getValue() {
            return value;
        }
    }

    public static final int CPU_ARCH_ABI64 = 0x01000000;

    public enum CpuType implements NumericEnumeration {
        CPU_TYPE_UNKNOWN(0, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_VAX(1, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_MC680x0(6, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_X86(7, Cpu.x86, Cpu.x64),
        CPU_TYPE_MIPS(8, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_MC98000(10, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_HPPA(11, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_ARM(12, Cpu.arm, Cpu.aarch64),
        CPU_TYPE_MC88000(13, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_SPARC(14, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_I860(15, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_ALPHA(16, Cpu.unknown, Cpu.unknown),
        CPU_TYPE_POWERPC(18, Cpu.ppc32, Cpu.ppc),
        ;

        private final int value;
        private final Cpu cpu32;
        private final Cpu cpu64;

        CpuType(final int value, final Cpu cpu32, final Cpu cpu64) {
            this.value = value;
            this.cpu32 = cpu32;
            this.cpu64 = cpu64;
        }

        private static final CpuType[] VALUES = values();

        public static CpuType forValue(int value) {
            return binarySearch(value, VALUES, CPU_TYPE_UNKNOWN);
        }

        public int getValue() {
            return value;
        }

        public Cpu toCpu(boolean is64) {
            return is64 ? cpu64 : cpu32;
        }
    }

    public enum Flag implements NumericEnumeration {
        MH_NOUNDEFS(0),
        MH_INCRLINK(1),
        MH_DYLDLINK(2),
        MH_BINDATLOAD(3),
        MH_PREBOUND(4),
        MH_SPLIT_SEGS(5),
        MH_LAZY_INIT(6),
        MH_TWOLEVEL(7),
        MH_FORCE_FLAT(8),
        MH_NOMULTIDEFS(9),
        MH_NOFIXPREBINDING(10),
        MH_PREBINDABLE(11),
        MH_ALLMODSBOUND(12),
        MH_SUBSECTIONS_VIA_SYMBOLS(13),
        MH_CANONICAL(14),
        MH_WEAK_DEFINES(15),
        MH_BINDS_TO_WEAK(16),
        MH_ALLOW_STACK_EXECUTION(17),
        MH_ROOT_SAFE(18),
        MH_SETUID_SAFE(19),
        MH_NO_REEXPORTED_DYLIBS(20),
        MH_PIE(21),
        MH_DEAD_STRIPPABLE_DYLIB(22),
        MH_HAS_TLV_DESCRIPTORS(23),
        MH_NO_HEAP_EXECUTION(24),
        MH_APP_EXTENSION_SAFE(25),
        MP_NLIST_OUTOFSYNC_WITH_DYLDINFO(26),
        ;

        private final int value;

        Flag(final int value) {
            this.value = value;
        }

        private static final Flag[] VALUES = values();

        public static Flag forValue(int value) {
            return binarySearch(value, VALUES, null);
        }

        public int getValue() {
            return value;
        }
    }

    public enum LoadCommand implements NumericEnumeration {
        LC_UNKNOWN(0),
        LC_SEGMENT(1),
        LC_SYMTAB(2),
        LC_SYMSEG(3),
        LC_THREAD(4),
        LC_UNIXTHREAD(5),
        LC_LOADFVMLIB(6),
        LC_IDFVMLIB(7),
        LC_IDENT(8), // obsolete
        LC_FVMFILE(9),
        LC_PREPAGE(0xa),
        LC_DYSYMTAB(0xb),
        LC_LOAD_DYLIB(0xc),
        LC_ID_DYLIB(0xd),
        LC_LOAD_DYLINKER(0xe),
        LC_ID_DYLINKER(0xf),
        LC_PREBOUND_DYLIB(0x10),
        LC_ROUTINES(0x11),
        LC_SUB_FRAMEWORK(0x12),
        LC_SUB_UMBRELLA(0x13),
        LC_SUB_CLIENT(0x14),
        LC_SUB_LIBRARY(0x15),
        LC_TWOLEVEL_HINTS(0x16),
        LC_PREBIND_CKSUM(0x17),
        LC_LOAD_WEAK_DYLIB(0x18),
        LC_SEGMENT_64(0x19),
        LC_ROUTINES_64(0x1a),
        LC_UUID(0x1b),
        LC_RPATH(0x1c),
        LC_CODE_SIGNATURE(0x1d),
        LC_SEGMENT_SPLIT_INFO(0x1e),
        LC_REEXPORT_DYLIB(0x1f),
        LC_LAZY_LOAD_DYLIB(0x20),
        LC_ENCRYPTION_INFO(0x21),
        LC_DYLD_INFO(0x22),
        LC_LOAD_UPWARD_DYLIB(0x23),
        LC_VERSION_MIN_MACOSX(0x24),
        LC_VERSION_MIN_IPHONEOS(0x25),
        LC_FUNCTION_STARTS(0x26),
        LC_DYLD_ENVIRONMENT(0x27),
        LC_MAIN(0x28),
        LC_DATA_IN_CODE(0x29),
        LC_SOURCE_VERSION(0x2A),
        LC_DYLIB_CODE_SIGN_DRS(0x2B),
        LC_ENCRYPTION_INFO_64(0x2C),
        LC_LINKER_OPTION(0x2D),
        LC_LINKER_OPTIMIZATION_HINT(0x2E),
        LC_VERSION_MIN_TVOS(0x2F),
        LC_VERSION_MIN_WATCHOS(0x30),
        LC_NOTE(0x31),
        LC_BUILD_VERSION(0x32),
        ;
        private final int value;

        LoadCommand(final int value) {
            this.value = value;
        }

        private static final LoadCommand[] VALUES = values();

        public static LoadCommand forValue(int value) {
            return binarySearch(value, VALUES, LC_UNKNOWN);
        }

        public int getValue() {
            return value;
        }
    }

    public interface NList {
        enum Type implements NumericEnumeration {
            UNDEF(0),
            ABS(1),
            TEXT(2),
            DATA(3),
            BSS(4),
            INDR(5),
            COMM(6),
            SECT(7),
            FILE_NAME(15),
            ;
            private final int value;
    
            Type(final int value) {
                this.value = value;
            }
    
            private static final Type[] VALUES = values();
    
            public static Type forValue(int value) {
                return binarySearch(value, VALUES, UNDEF);
            }
    
            public int getValue() {
                return value;
            }
        }
    }

    interface NumericEnumeration {
        int getValue();
    }

    static <T extends NumericEnumeration & Comparable<T>> T binarySearch(int value, T[] sortedItems, T defVal) {
        // the standard binary search
        int low = 0;
        int high = sortedItems.length - 1;
        while (low <= high) {
            final int mid = (low + high) >>> 1;
            final T item = sortedItems[mid];
            final int val = item.getValue();
            if (val < value) {
                low = mid + 1;
            } else if (val > value) {
                high = mid - 1;
            } else {
                return item;
            }
        }
        return defVal;
    }
}
