package org.qbicc.machine.file.elf;

import java.nio.ByteOrder;
import java.util.Objects;

import org.qbicc.machine.arch.Cpu;

/**
 *
 */
public final class Elf {
    private Elf() {
    }

    public interface NumericEnumeration {
        int getValue();
    }

    public interface Flag extends NumericEnumeration {
        static Flag forValue(int value) {
            return unknown(value);
        }

        static Flag unknown(int value) {
            return () -> value;
        }

        enum Arm implements Flag {
            REL_EXEC(0),
            HAS_ENTRY(1),
            INTERWORK(2),
            APCS_26(3),
            APCS_FLOAT(4),
            PIC(5),
            ALIGN8(6),
            NEW_ABI(7),
            OLD_ABI(8),
            SOFT_FLOAT(9),
            VFP_FLOAT(10),
            MAVERICK_FLOAT(11),
            LE8(22),
            BE8(23),
            ;

            private static final Arm[] VALUES = values();

            private final int value;

            public static Arm forValue(int value) {
                return binarySearch(value, VALUES);
            }

            Arm(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public interface Class extends NumericEnumeration {
        static Class forValue(int value) {
            Class elfClass = Std.forValue(value);
            return elfClass == null ? unknown(value) : elfClass;
        }

        static Class unknown(int value) {
            return () -> value;
        }

        enum Std implements Class {
            NONE(0),
            _32(1),
            _64(2),
            ;

            private static final Std[] VALUES = values();

            private final int value;

            public static Std forValue(int value) {
                return binarySearch(value, VALUES);
            }

            Std(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public interface Data extends NumericEnumeration {
        default ByteOrder byteOrder() {
            throw new IllegalArgumentException("No known byte order for elf data " + this);
        }

        static Data forValue(int value) {
            Data elfData = Std.forValue(value);
            return elfData == null ? unknown(value) : elfData;
        }

        static Data forByteOrder(ByteOrder bo) {
            Objects.requireNonNull(bo);
            assert bo == ByteOrder.BIG_ENDIAN || bo == ByteOrder.LITTLE_ENDIAN;
            return bo == ByteOrder.BIG_ENDIAN ? Std.BE : Std.LE;
        }

        static Data unknown(int value) {
            return () -> value;
        }

        enum Std implements Data {
            NONE(0),
            LE(1),
            BE(2),
            ;

            private static final Std[] VALUES = values();

            private final int value;

            public static Std forValue(int value) {
                return binarySearch(value, VALUES);
            }

            Std(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }

            public ByteOrder byteOrder() {
                return this == LE ? ByteOrder.LITTLE_ENDIAN : this == BE ? ByteOrder.BIG_ENDIAN : Data.super.byteOrder();
            }
        }
    }

    public interface OsAbi extends NumericEnumeration {
        static OsAbi forValue(int value) {
            OsAbi osAbi = Std.forValue(value);
            return osAbi == null ? unknown(value) : osAbi;
        }

        static OsAbi unknown(int value) {
            return () -> value;
        }

        enum Std implements OsAbi {
            SYSV(0),
            HPUX(1),
            NETBSD(2),
            LINUX(3),
            GNU_HURD(4),
            SOLARIS(6),
            AIX(7),
            IRIX(8),
            FREEBSD(9),
            TRU64(0x0A),
            NOVELL_MODESTO(0x0B),
            OPENBSD(0x0C),
            OPENVMS(0x0D),
            NONSTOP_KERNEL(0x0E),
            AROS(0x0F),
            FENIX_OS(0x10),
            CLOUDABI(0x11),
            ;

            private static final Std[] VALUES = values();

            private final int value;

            public static Std forValue(int value) {
                return binarySearch(value, VALUES);
            }

            Std(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public interface Type extends NumericEnumeration {
        static Type forValue(int value) {
            Type type = Std.forValue(value);
            return type == null ? unknown(value) : type;
        }

        static Type unknown(int value) {
            return () -> value;
        }

        enum Std implements Type {
            NONE(0),
            REL(1),
            EXEC(2),
            DYN(3),
            CORE(4),
            ;

            private static final Std[] VALUES = values();

            private final int value;

            public static Std forValue(int value) {
                return binarySearch(value, VALUES);
            }

            Std(final int value) {
                this.value = value;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public interface Machine extends NumericEnumeration {
        static Machine forValue(int value) {
            Machine machine = Std.forValue(value);
            return machine == null ? unknown(value) : machine;
        }

        static Machine unknown(int value) {
            return () -> value;
        }

        default long maskFlags(long original) {
            return original;
        }

        default Flag decodeFlag(int value) {
            return Flag.unknown(value);
        }

        default Section.Type decodeSectionType(int value) {
            return Section.Type.forValue(value);
        }

        default Section.Flag decodeSectionFlag(int value) {
            return Section.Flag.forValue(value);
        }

        default int getSpecificValue(long value) {
            return 0;
        }

        default long mergeSpecificValue(long value, int specificValue) {
            return value;
        }

        default Relocation.Type decodeRelocationType(int rawType) {
            return Relocation.Type.unknown(rawType);
        }

        default Cpu toCpu() {
            return Cpu.unknown;
        }

        enum Std implements Machine {
            NONE(0, Cpu.unknown),
            M32(1, Cpu.unknown),
            SPARC(2, Cpu.unknown),
            X86(3, Cpu.x86),
            _68K(4, Cpu.unknown),
            _88K(5, Cpu.unknown),
            IA_MCU(6, Cpu.unknown),
            _860(7, Cpu.unknown),
            MIPS(8, Cpu.unknown),
            S370(9, Cpu.unknown),
            MIPS_RS3_LE(0x0A, Cpu.unknown),
            PARISC(0x0F, Cpu.unknown),
            VPP500(0x11, Cpu.unknown),
            SPARC32PLUS(0x12, Cpu.unknown),
            _960(0x13, Cpu.unknown),
            PPC(0x14, Cpu.ppc32),
            PPC64(0x15, Cpu.ppc),
            S390(0x16, Cpu.unknown),
            ARM(0x28, Cpu.arm) {
                public long maskFlags(final long original) {
                    return original & 0xffffff;
                }

                public Flag decodeFlag(final int value) {
                    return Flag.Arm.forValue(value);
                }

                public Section.Type decodeSectionType(final int value) {
                    Section.Type type = Section.Type.Arm.forValue(value);
                    return type == null ? super.decodeSectionType(value) : type;
                }

                public Section.Flag decodeSectionFlag(final int value) {
                    Section.Flag.Arm flag = Section.Flag.Arm.forValue(value);
                    return flag == null ? super.decodeSectionFlag(value) : flag;
                }

                public Relocation.Type decodeRelocationType(final int rawType) {
                    return Relocation.Type.Arm.forValue(rawType);
                }

                public int getSpecificValue(final long value) {
                    return (int) value >>> 24;
                }

                public long mergeSpecificValue(final long value, final int specificValue) {
                    return (specificValue & 0xff) << 24 | value & 0xffffff;
                }
            },
            SUPER_H(0x2A, Cpu.unknown),
            SPARCV9(0x2B, Cpu.unknown),
            IA_64(0x32, Cpu.unknown),
            X86_64(0x3E, Cpu.x64),
            OPENRISC(0x5C, Cpu.unknown),
            AARCH64(0xB7, Cpu.aarch64),
            CUDA(0xBE, Cpu.unknown),
            RISC_V(0xF3, Cpu.unknown),
            ;

            private static final Std[] VALUES = values();

            private final int value;
            private final Cpu cpu;

            public static Std forValue(int value) {
                return binarySearch(value, VALUES);
            }

            public Cpu toCpu() {
                return cpu;
            }

            Std(final int value, final Cpu cpu) {
                this.value = value;
                this.cpu = cpu;
            }

            public int getValue() {
                return value;
            }
        }
    }

    public static final class Program {
        private Program() {
        }

        public interface Flag extends NumericEnumeration {
            static Flag forValue(int value) {
                Flag flag = Std.forValue(value);
                return flag == null ? unknown(value) : flag;
            }

            static Flag unknown(int value) {
                return () -> value;
            }

            enum Std implements Flag {
                EXEC(1),
                WRITE(2),
                READ(3),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }

        public interface Type extends NumericEnumeration {
            static Type forValue(int value) {
                Type programType = Std.forValue(value);
                if (programType == null)
                    programType = Gnu.forValue(value);
                if (programType == null)
                    programType = SunW.forValue(value);
                return programType == null ? unknown(value) : programType;
            }

            static Type unknown(int value) {
                return () -> value;
            }

            enum Std implements Type {
                NULL(0),
                LOAD(1),
                DYNAMIC(2),
                INTERP(3),
                NOTE(4),
                PHDR(6),
                TLS(7),
                NUM(8),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum Gnu implements Type {
                EH_FRAME(0x6474e550),
                STACK(0x6474e551),
                REL_RDONLY(0x647e552),
                ;

                private static final Gnu[] VALUES = values();

                private final int value;

                public static Gnu forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Gnu(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum SunW implements Type {
                BSS(0x6ffffffa),
                STACK(0x6ffffffb),
                ;

                private static final SunW[] VALUES = values();

                private final int value;

                public static SunW forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                SunW(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }
    }

    static <T extends NumericEnumeration & Comparable<T>> T binarySearch(int value, T[] sortedItems) {
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
        return null;
    }

    public static final class Relocation {
        private Relocation() {
        }

        public interface Type extends NumericEnumeration {
            static Type unknown(int value) {
                return () -> value;
            }

            enum Arm implements Type {
                NONE(0),
                /**
                 * PC-relative 26-bit branch (ARM)
                 */
                @Deprecated
                PC24(1),
                /**
                 * Absolute 32-bit (Data)
                 */
                ABS32(2),
                /**
                 * Relative 32-bit (Data)
                 */
                REL32(3),
                /**
                 * PC-relative 13-bit branch (Arm, group 0)
                 */
                LDR_PC_G0(4),
                /**
                 * Absolute 16-bit (Data), sign-extended
                 */
                ABS16(5),
                /**
                 * Absolute 12-bit (Arm, Data)
                 */
                ABS12(6),
                /**
                 * Absolute 5-bit (Thumb16, mask 0x7C for LDR/STR)
                 */
                THM_ABS5(7),
                /**
                 * Absolute 8-bit (Data), sign extended
                 */
                ABS8(8),
                /**
                 * Symbol(?) branch relative(?) (Data)
                 */
                SBREL32(9),
                /**
                 * PC-relative 24-bit Thumb call for BL (Thumb32)
                 */
                THM_CALL(10),
                /**
                 * PC-relative 8-bit (mask 0x3FC for LDR/ADD/ADR) (Thumb16)
                 */
                THM_PC8(11),
                /**
                 * (Dynamic, Data)
                 */
                BREL_ADJ(12),
                /**
                 * (Dynamic, Data)
                 */
                TLS_DESC(13),
                // 14-16 reserved
                /**
                 * (Dynamic, Data)
                 */
                TLS_DTPMOD32(17),
                /**
                 * (Dynamic, Data)
                 */
                TLS_DTPOFF32(18),
                /**
                 * (Dynamic, Data)
                 */
                TLS_TPOFF32(19),
                /**
                 * Copy symbol (Dynamic, Misc.)
                 */
                COPY(20),
                /**
                 * Create global offset table entry (Dynamic, Data)
                 */
                GLOB_DAT(21),
                /**
                 * (Dynamic, Data)
                 */
                JUMP_SLOT(22),
                /**
                 * (Dynamic, Data)
                 */
                RELATIVE(23),
                /**
                 * (Static, Data)
                 */
                GOTOFF32(24),
                /**
                 * (Static, Data)
                 */
                BASE_PREL(25),
                /**
                 * (Static, Data)
                 */
                GOT_BREL(26),
                /**
                 * (Arm)
                 */
                @Deprecated
                PLT32(27),
                /**
                 * PC-relative 24-bit Arm call for BL (Arm)
                 */
                CALL(28),
                /**
                 * (Static, Arm)
                 */
                JUMP24(29),
                /**
                 * (Static, Thumb32)
                 */
                THM_JUMP24(30),
                /**
                 * (Static, Data)
                 */
                BASE_ABS(31),
                /**
                 * (Deprecated, Arm)
                 */
                @Deprecated
                LDR_SBREL_11_0_NC(35),
                /**
                 * (Deprecated, Arm)
                 */
                @Deprecated
                ALU_SBREL_19_12_NC(36),
                /**
                 * (Deprecated, Arm)
                 */
                @Deprecated
                ALU_SBREL_27_20_CK(37),
                /**
                 * (Static, Miscellaneous)
                 */
                TARGET1(38),
                /**
                 * (Deprecated, Data)
                 */
                @Deprecated
                SBREL31(39),
                /**
                 * (Static, Miscellaneous)
                 */
                V4BX(40),
                /**
                 * (Static, Miscellaneous)
                 */
                TARGET2(41),
                /**
                 * (Static, Data)
                 */
                PREL31(42),
                /**
                 * (Static, Arm)
                 */
                MOVW_ABS_NC(43),
                /**
                 * (Static, Arm)
                 */
                MOVT_ABS(44),
                /**
                 * (Static, Arm)
                 */
                MOVW_PREL_NC(45),
                /**
                 * (Static, Arm)
                 */
                MOVT_PREL(46),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVW_ABS_NC(47),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVT_ABS(48),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVW_PREL_NC(49),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVT_PREL(50),
                /**
                 * (Static, Thumb32)
                 */
                THM_JUMP19(51),
                /**
                 * (Static, Thumb16)
                 */
                THM_JUMP6(52),
                /**
                 * (Static, Thumb32)
                 */
                THM_ALU_PREL_11_0(53),
                /**
                 * (Static, Thumb32)
                 */
                THM_PC12(54),
                /**
                 * (Static, Data)
                 */
                ABS32_NOI(55),
                /**
                 * (Static, Data)
                 */
                REL32_NOI(56),
                /**
                 * (Static, Arm)
                 */
                ALU_PC_G0_NC(57),
                /**
                 * (Static, Arm)
                 */
                ALU_PC_G0(58),
                /**
                 * (Static, Arm)
                 */
                ALU_PC_G1_NC(59),
                /**
                 * (Static, Arm)
                 */
                ALU_PC_G1(60),
                /**
                 * (Static, Arm)
                 */
                ALU_PC_G2(61),
                /**
                 * (Static, Arm)
                 */
                LDR_PC_G1(62),
                /**
                 * (Static, Arm)
                 */
                LDR_PC_G2(63),
                /**
                 * (Static, Arm)
                 */
                LDRS_PC_G0(64),
                /**
                 * (Static, Arm)
                 */
                LDRS_PC_G1(65),
                /**
                 * (Static, Arm)
                 */
                LDRS_PC_G2(66),
                /**
                 * (Static, Arm)
                 */
                LDC_PC_G0(67),
                /**
                 * (Static, Arm)
                 */
                LDC_PC_G1(68),
                /**
                 * (Static, Arm)
                 */
                LDC_PC_G2(69),
                /**
                 * (Static, Arm)
                 */
                ALU_SB_G0_NC(70),
                /**
                 * (Static, Arm)
                 */
                ALU_SB_G0(71),
                /**
                 * (Static, Arm)
                 */
                ALU_SB_G1_NC(72),
                /**
                 * (Static, Arm)
                 */
                ALU_SB_G1(73),
                /**
                 * (Static, Arm)
                 */
                ALU_SB_G2(74),
                /**
                 * (Static, Arm)
                 */
                LDR_SB_G0(75),
                /**
                 * (Static, Arm)
                 */
                LDR_SB_G1(76),
                /**
                 * (Static, Arm)
                 */
                LDR_SB_G2(77),
                /**
                 * (Static, Arm)
                 */
                LDRS_SB_G0(78),
                /**
                 * (Static, Arm)
                 */
                LDRS_SB_G1(79),
                /**
                 * (Static, Arm)
                 */
                LDRS_SB_G2(80),
                /**
                 * (Static, Arm)
                 */
                LDC_SB_G0(81),
                /**
                 * (Static, Arm)
                 */
                LDC_SB_G1(82),
                /**
                 * (Static, Arm)
                 */
                LDC_SB_G2(83),
                /**
                 * (Static, Arm)
                 */
                MOVW_BREL_NC(84),
                /**
                 * (Static, Arm)
                 */
                MOVT_BREL(85),
                /**
                 * (Static, Arm)
                 */
                MOVW_BREL(86),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVW_BREL_NC(87),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVT_BREL(88),
                /**
                 * (Static, Thumb32)
                 */
                THM_MOVW_BREL(89),
                /**
                 * (Static, Data)
                 */
                TLS_GOTDESC(90),
                /**
                 * (Static, Arm)
                 */
                TLS_CALL(91),
                /**
                 * (Static, Arm)
                 */
                TLS_DESCSEQ(92),
                /**
                 * (Static, Thumb32)
                 */
                THM_TLS_CALL(93),
                /**
                 * (Static, Data)
                 */
                PLT32_ABS(94),
                /**
                 * (Static, Data)
                 */
                GOT_ABS(95),
                /**
                 * (Static, Data)
                 */
                GOT_PREL(96),
                /**
                 * (Static, Arm)
                 */
                GOT_BREL12(97),
                /**
                 * (Static, Arm)
                 */
                GOTOFF12(98),
                /**
                 * (Static, Miscellaneous)
                 */
                GOTRELAX(99),
                /**
                 * (Deprecated, Data)
                 */
                @Deprecated
                GNU_VTENTRY(100),
                /**
                 * (Deprecated, Data)
                 */
                @Deprecated
                GNU_VTINHERIT(101),
                /**
                 * (Static, Thumb16)
                 */
                THM_JUMP11(102),
                /**
                 * (Static, Thumb16)
                 */
                THM_JUMP8(103),
                /**
                 * (Static, Data)
                 */
                TLS_GD32(104),
                /**
                 * (Static, Data)
                 */
                TLS_LDM32(105),
                /**
                 * (Static, Data)
                 */
                TLS_LDO32(106),
                /**
                 * (Static, Data)
                 */
                TLS_IE32(107),
                /**
                 * (Static, Data)
                 */
                TLS_LE32(108),
                /**
                 * (Static, Arm)
                 */
                TLS_LDO12(109),
                /**
                 * (Static, Arm)
                 */
                TLS_LE12(110),
                /**
                 * (Static, Arm)
                 */
                TLS_IE12GP(111),
                /**
                 * (Static, Thumb16)
                 */
                THM_TLS_DESCSEQ16(129),
                /**
                 * (Static, Thumb32)
                 */
                THM_TLS_DESCSEQ32(130),
                /**
                 * (Static, Thumb32)
                 */
                THM_GOT_BREL12(131),
                /**
                 * (Static, Thumb16)
                 */
                THM_ALU_ABS_G0_NC(132),
                /**
                 * (Static, Thumb16)
                 */
                THM_ALU_ABS_G1_NC(133),
                /**
                 * (Static, Thumb16)
                 */
                THM_ALU_ABS_G2_NC(134),
                /**
                 * (Static, Thumb16)
                 */
                THM_ALU_ABS_G3(135),
                /**
                 * (Static, Arm)
                 */
                THM_BF16(136),
                /**
                 * (Static, Arm)
                 */
                THM_BF12(137),
                /**
                 * (Static, Arm)
                 */
                THM_BF18(138),

                ;

                private static final Arm[] VALUES = values();

                private final int value;

                public static Arm forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Arm(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }
    }

    public static final class Section {

        private Section() {
        }

        public static final class Index {
            private Index() {
            }

            public static final int UNDEF = 0;
            public static final int ABSOLUTE = 0xFFF1;
            public static final int COMMON = 0xFFF2;
            public static final int EXTENDED = 0xFFFF;
        }

        public interface Type extends NumericEnumeration {
            static Type forValue(int value) {
                Type type = Std.forValue(value);
                return type == null ? unknown(value) : type;
            }

            static Type unknown(int value) {
                return () -> value;
            }

            enum Std implements Type {
                NULL(0),
                PROG_BITS(1),
                SYM_TAB(2),
                STR_TAB(3),
                REL_A(4),
                HASH(5),
                DYNAMIC(6),
                NOTE(7),
                NO_BITS(8),
                REL(9),
                SHLIB(0x0A),
                DYN_SYM(0x0B),
                INIT_ARRAY(0x0E),
                FINI_ARRAY(0x0F),
                PREINIT_ARRAY(0x10),
                GROUP(0x11),
                SYM_TAB_SHNDX(0x12),
                NUM(0x13),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum Gnu implements Type {
                ATTRIBUTES(0x6FFFFFF5),
                HASH(0x6FFFFFF6),
                LIB_LIST(0x6FFFFFF7),
                CHECKSUM(0x6FFFFFF8),
                VERSION_DEF(0x6FFFFFFD),
                VERSION_NEEDS(0x6FFFFFFE),
                VERSION_SYM(0x6FFFFFFF),
                ;

                private static final Gnu[] VALUES = values();

                private final int value;

                public static Gnu forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Gnu(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum Sun implements Type {
                MOVE(0x6FFFFFFA),
                COM_DAT(0x6FFFFFFB),
                SYM_INFO(0x6FFFFFFC),
                ;

                private static final Sun[] VALUES = values();

                private final int value;

                public static Sun forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Sun(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }

            }

            enum Arm implements Type {
                EX_IDX(0x70000001), // ARM unwind section
                PREEMPT_MAP(0x70000002), // Preemption details
                ATTRIBUTES(0x70000003), // ARM attributes
                DEBUG_OVERLAY(0x70000004),
                OVERLAY_SECTION(0x70000005),
                ;

                private static final Arm[] VALUES = values();

                private final int value;

                public static Arm forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Arm(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }

        public interface Flag extends NumericEnumeration {
            int OS_SPECIFIC = 20;
            int CPU_SPECIFIC = 28;

            static Flag forValue(int value) {
                Flag type = Std.forValue(value);
                return type == null ? unknown(value) : type;
            }

            static Flag unknown(int value) {
                return () -> value;
            }

            enum Std implements Flag {
                WRITE(0),
                ALLOC(1),
                EXEC_INSTR(2),
                MERGE(4),
                STRINGS(5),
                INFO_LINK(6),
                LINK_ORDER(7),
                OS_NONCONFORMING(8),
                GROUP(9),
                TLS(10),
                COMPRESSED(11),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum Arm implements Flag {
                PURE_CODE(CPU_SPECIFIC),
                ;

                private static final Arm[] VALUES = values();

                private final int value;

                public static Arm forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Arm(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }
    }

    public static final class Symbol {
        private Symbol() {
        }

        // special "section" numbers

        public interface Binding extends NumericEnumeration {
            static Binding forValue(int value) {
                Binding binding = Std.forValue(value);
                if (binding == null) {
                    binding = Gnu.forValue(value);
                }
                return binding == null ? unknown(value) : binding;
            }

            static Binding unknown(int value) {
                return () -> value;
            }

            enum Std implements Binding {
                LOCAL(0),
                GLOBAL(1),
                WEAK(2),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }

            enum Gnu implements Binding {
                UNIQUE(10),
                ;

                private static final Gnu[] VALUES = values();

                private final int value;

                public static Gnu forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Gnu(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }

        public interface Type extends NumericEnumeration {
            static Type forValue(int value) {
                Type type = Std.forValue(value);
                return type == null ? unknown(value) : type;
            }

            static Type unknown(int value) {
                return () -> value;
            }

            enum Std implements Type {
                NO_TYPE(0),
                OBJECT(1),
                FUNC(2),
                SECTION(3),
                FILE(4),
                COMMON(5),
                TLS(6),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }

        public interface Visibility extends NumericEnumeration {
            static Visibility forValue(int value) {
                Visibility visibility = Std.forValue(value);
                return visibility == null ? unknown(value) : visibility;
            }

            static Visibility unknown(int value) {
                return () -> value;
            }

            enum Std implements Visibility {
                DEFAULT(0),
                INTERNAL(1),
                HIDDEN(2),
                PROTECTED(3),
                ;

                private static final Std[] VALUES = values();

                private final int value;

                public static Std forValue(int value) {
                    return binarySearch(value, VALUES);
                }

                Std(final int value) {
                    this.value = value;
                }

                public int getValue() {
                    return value;
                }
            }
        }
    }
}
