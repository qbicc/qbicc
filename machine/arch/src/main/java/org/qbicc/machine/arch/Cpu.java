package org.qbicc.machine.arch;

import java.nio.ByteOrder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.impl.block.factory.Functions;

/**
 * The possible CPU architectures.
 */
public enum Cpu implements PlatformComponent {
    unknown(4, ByteOrder.LITTLE_ENDIAN, Set.of()),
    x64(8, ByteOrder.LITTLE_ENDIAN, "x86_64", Set.of("x86_64", "amd64")) {
        @Override
        public Register registerForId(int dwarfId) {
            return Register.x64.forId(dwarfId);
        }
    },
    x86(4, ByteOrder.LITTLE_ENDIAN, Set.of("i386", "i486", "i586", "i686")) {
        @Override
        public Register registerForId(int dwarfId) {
            return Register.x86.forId(dwarfId);
        }
    },
    aarch64(8, ByteOrder.LITTLE_ENDIAN, Set.of("arm64")) {
        @Override
        public Register registerForId(int dwarfId) {
            return Register.aarch64.forId(dwarfId);
        }
    },
    arm(4, ByteOrder.LITTLE_ENDIAN, Set.of("armv7", "armv7hl")) {
        @Override
        public Set<SubArch> subArchitectures() {
            // doesn't really copy
            return Set.copyOf(SubArch.arm.values);
        }

        @Override
        public Register registerForId(int dwarfId) {
            return Register.arm.forId(dwarfId);
        }

        @Override
        public SubArch subArchitecture(String name) {
            return SubArch.arm.forName(name);
        }
    },
    ppc32(4, ByteOrder.BIG_ENDIAN, Set.of("ppc32be")),
    ppc32le(4, ByteOrder.LITTLE_ENDIAN, Set.of()),
    ppc(8, ByteOrder.BIG_ENDIAN, Set.of("ppc64", "ppcbe", "ppc64be")),
    ppcle(8, ByteOrder.LITTLE_ENDIAN, Set.of("ppc64le")),
    wasm32(4, ByteOrder.LITTLE_ENDIAN, Set.of("wasm")),
    ;

    private final int wordSize;
    private final String llvmName;
    private final Set<String> aliases;
    private final ByteOrder byteOrder;

    Cpu(final int wordSize, ByteOrder byteOrder, final String llvmName, final Set<String> aliases) {
        this.wordSize = wordSize;
        this.byteOrder = byteOrder;
        this.llvmName = llvmName;
        this.aliases = aliases;
    }

    Cpu(final int wordSize, ByteOrder byteOrder, final Set<String> aliases) {
        this.wordSize = wordSize;
        this.byteOrder = byteOrder;
        this.llvmName = name();
        this.aliases = aliases;
    }

    private static final Map<String, Cpu> index = Indexer.index(Cpu.class);

    public String simpleName() {
        return name();
    }

    public String llvmName() {
        return llvmName;
    }

    public static Cpu forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), unknown);
    }

    public static Set<String> names() {
        return index.keySet();
    }

    @Override
    public Set<String> aliases() {
        return aliases;
    }

    public Set<SubArch> subArchitectures() {
        return Set.of();
    }

    public SubArch subArchitecture(String name) {
        return null;
    }

    public ByteOrder byteOrder() {
        return byteOrder;
    }

    /**
     * Get the CPU register word size, in bytes.  This is used to determine how to
     * handle 64-bit integer values.
     *
     * @return the CPU word size
     */
    public int wordSize() {
        return wordSize;
    }

    public Register registerForId(int dwarfId) {
        throw new IllegalArgumentException("No register exists on " + this + " with ID " + dwarfId);
    }

    public sealed interface Register permits Register.aarch64, Register.arm, Register.x86, Register.x64 {
        /**
         * Get the human-readable name of this register.
         *
         * @return the register name
         */
        String name();

        /**
         * Get the DWARF identifier for this register.
         *
         * @return the DWARF identifier
         */
        int dwarfId();

        /**
         * Registers for 64-bit ARM (AArch64). Keep in order.
         */
        enum aarch64 implements Register {
            // 0

            x0,
            x1,
            x2,
            x3,
            x4,
            x5,
            x6,
            x7,
            x8,
            x9,
            x10,
            x11,
            x12,
            x13,
            x14,
            x15,
            x16,
            x17,
            x18,
            x19,
            x20,
            x21,
            x22,
            x23,
            x24,
            x25,
            x26,
            x27,
            x28,
            x29,
            x30,

            // 31

            sp,
            pc,

            elr_mode,

            ra_sign_state,

            tpidrro_el0,
            tpidr_el0,
            tpidr_el1,
            tpidr_el2,
            tpidr_el3,

            // 40-63 reserved or beta

            V0(64),
            V1(65),
            V2(66),
            V3(67),
            V4(68),
            V5(69),
            V6(70),
            V7(71),
            V8(72),
            V9(73),
            V10(74),
            V11(75),
            V12(76),
            V13(77),
            V14(78),
            V15(79),
            V16(80),
            V17(81),
            V18(82),
            V19(83),
            V20(84),
            V21(85),
            V22(86),
            V23(87),
            V24(88),
            V25(89),
            V26(90),
            V27(91),
            V28(92),
            V29(93),
            V30(94),
            V31(95),

            // 96-127 reserved or beta

            ;

            // alias
            public static final aarch64 ip = pc;

            public static final List<aarch64> values = List.of(values());

            private static final ImmutableIntObjectMap<aarch64> index = IntObjectMaps.immutable.from(values, Register::dwarfId, Functions.identity());

            private final int dwarfId;

            public static aarch64 forId(final int dwarfId) {
                aarch64 reg = index.get(dwarfId);
                if (reg == null) {
                    throw new NoSuchElementException("No register with ID " + dwarfId);
                }
                return reg;
            }

            @Override
            public String toString() {
                return "%" + name();
            }

            aarch64(int dwarfId) {
                this.dwarfId = dwarfId;
            }

            aarch64() {
                this.dwarfId = ordinal();
            }

            @Override
            public int dwarfId() {
                return dwarfId;
            }
        }

        /**
         * Registers for 32-bit ARM. Keep in order.
         */
        enum arm implements Register {
            // 0

            r0,
            r1,
            r2,
            r3,
            r4,
            r5,
            r6,
            r7,
            r8,
            r9,
            r10,
            r11,
            r12,
            r13,
            r14,
            r15,

            // 16-127 obsolete, reserved, or vendor-specific

            spsr(128),
            spsr_fiq(129),
            spsr_irq(130),
            spsr_abt(131),
            spsr_und(132),
            spsr_svc(133),

            // 134-142 reserved

            ra_auth_code(134),

            // 134-16383 reserved, supervisor-only, or vendor-specific
            ;

            // alias
            public static final arm pc = r15;
            public static final arm ip = r15;
            public static final arm lr = r14;

            public static final List<arm> values = List.of(values());

            private static final ImmutableIntObjectMap<arm> index = IntObjectMaps.immutable.from(values, Register::dwarfId, Functions.identity());

            private final int dwarfId;

            public static arm forId(final int dwarfId) {
                arm reg = index.get(dwarfId);
                if (reg == null) {
                    throw new NoSuchElementException("No register with ID " + dwarfId);
                }
                return reg;
            }

            @Override
            public String toString() {
                return "%" + name();
            }

            arm(int dwarfId) {
                this.dwarfId = dwarfId;
            }

            arm() {
                this.dwarfId = ordinal();
            }

            @Override
            public int dwarfId() {
                return dwarfId;
            }
        }

        /**
         * Registers for x64. Keep in order.
         */
        enum x64 implements Register {
            // 0

            rax,
            rdx,
            rcx,
            rbx,
            rsi,
            rdi,
            rbp,
            rsp,

            // 8

            r8,
            r9,
            r10,
            r11,
            r12,
            r13,
            r14,
            r15,

            // 16

            rip,

            // 17-32

            xmm0,
            xmm1,
            xmm2,
            xmm3,
            xmm4,
            xmm5,
            xmm6,
            xmm7,
            xmm8,
            xmm9,
            xmm10,
            xmm11,
            xmm12,
            xmm13,
            xmm14,
            xmm15,

            // 33-40

            st0,
            st1,
            st2,
            st3,
            st4,
            st5,
            st6,
            st7,

            // 41-48

            mm0,
            mm1,
            mm2,
            mm3,
            mm4,
            mm5,
            mm6,
            mm7,

            // 49

            rFLAGS,

            // 50-55

            es,
            cs,
            ss,
            ds,
            fs,
            gs,

            // 56-57 reserved

            fs_base(58),
            gs_base(59),

            // 60-61 reserved

            tr(62),
            ldtr(63),
            mxcsr(64),
            fcw(65),
            fsw(66),

            xmm16(67),
            xmm17(68),
            xmm18(69),
            xmm19(70),
            xmm20(71),
            xmm21(72),
            xmm22(73),
            xmm23(74),
            xmm24(75),
            xmm25(76),
            xmm26(77),
            xmm27(78),
            xmm28(79),
            xmm29(80),
            xmm30(81),
            xmm31(82),

            // 83-117 reserved

            k0(118),
            k1(119),
            k2(120),
            k3(121),
            k4(122),
            k5(123),
            k6(124),
            k7(125),

            // 126-129 reserved
            ;

            public static final List<x64> values = List.of(values());

            private static final ImmutableIntObjectMap<x64> index = IntObjectMaps.immutable.from(values, Register::dwarfId, Functions.identity());

            private final int dwarfId;

            public static x64 forId(final int dwarfId) {
                x64 reg = index.get(dwarfId);
                if (reg == null) {
                    throw new NoSuchElementException("No register with ID " + dwarfId);
                }
                return reg;
            }

            @Override
            public String toString() {
                return "%" + name();
            }

            x64(int dwarfId) {
                this.dwarfId = dwarfId;
            }

            x64() {
                this.dwarfId = ordinal();
            }

            @Override
            public int dwarfId() {
                return dwarfId;
            }
        }
        /**
         * Registers for x86 (i386). Keep in order.
         */
        enum x86 implements Register  {
            // 0

            eax,
            ecx,
            edx,
            ebx,
            esp,
            ebp,
            esi,
            edi,

            // 8

            ip,

            // 9

            eFLAGS,

            // 10 reserved

            st0(11),
            st1(12),
            st2(13),
            st3(14),
            st4(15),
            st5(16),
            st6(17),
            st7(18),

            // 19-20 reserved

            xmm0(21),
            xmm1(22),
            xmm2(23),
            xmm3(24),
            xmm4(25),
            xmm5(26),
            xmm6(27),
            xmm7(28),

            mm0(29),
            mm1(30),
            mm2(31),
            mm3(32),
            mm4(33),
            mm5(34),
            mm6(35),
            mm7(36),

            // 37-38 reserved

            mxcsr(39),

            es(40),
            cs(41),
            ss(42),
            ds(43),
            fs(44),
            gs(45),

            // 46-47 reserved

            tr(48),
            ldtr(49),

            // 50-92 reserved
            ;

            public static final List<x86> values = List.of(values());

            private static final ImmutableIntObjectMap<x86> index = IntObjectMaps.immutable.from(values, Register::dwarfId, Functions.identity());

            private final int dwarfId;

            public static x86 forId(final int dwarfId) {
                x86 reg = index.get(dwarfId);
                if (reg == null) {
                    throw new NoSuchElementException("No register with ID " + dwarfId);
                }
                return reg;
            }

            @Override
            public String toString() {
                return "%" + name();
            }

            x86(int dwarfId) {
                this.dwarfId = dwarfId;
            }

            x86() {
                this.dwarfId = ordinal();
            }

            @Override
            public int dwarfId() {
                return dwarfId;
            }
        }
    }

    public sealed interface SubArch extends PlatformComponent permits SubArch.arm {
        String name();

        /**
         * ARM sub-architectures.
         */
        enum arm implements SubArch {

            v4(ArmProfile.Classic, Set.of(), Set.of()),
            v4t(ArmProfile.Classic, Set.of(v4), Set.of()),
            v5te(ArmProfile.Classic, Set.of(v4t), Set.of()),
            v6(ArmProfile.Classic, Set.of(v5te), Set.of()),
            v6_m(ArmProfile.Microcontroller, Set.of(v5te), Set.of("v6-m")),
            v7(ArmProfile.Application, Set.of(v6), Set.of()),
            v7_m(ArmProfile.Microcontroller, Set.of(v6_m), Set.of("v7-m")),
            v7e_m(ArmProfile.Microcontroller, Set.of(v7_m), Set.of("v7e-m")),
            v7_r(ArmProfile.RealTime, Set.of(v6), Set.of("v7-r")),
        ;
            public final static Set<SubArch.arm> values = Set.of(values());

            private final Set<SubArch.arm> incorporates;
            private final ArmProfile profile;
            private final Set<String> aliases;

            arm(final String alias, final ArmProfile profile) {
                this.profile = profile;
                this.incorporates = Set.of();
                this.aliases = Set.of(alias);
            }

            arm(final ArmProfile profile, Set<SubArch.arm> incorporates, Set<String> aliases) {
                this.profile = profile;
                this.incorporates = incorporates;
                this.aliases = aliases;
            }

            public boolean incorporates(SubArch.arm arch) {
                if (this == arch) {
                    return true;
                }
                if (incorporates.contains(arch)) {
                    return true;
                }
                for (SubArch.arm incorporated : incorporates) {
                    if (incorporated.incorporates(arch)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public Set<String> aliases() {
                return aliases;
            }

            public ArmProfile getProfile() {
                return profile;
            }

            private static final Map<String, SubArch.arm> index = Indexer.index(SubArch.arm.class);

            public static SubArch.arm forName(String name) {
                return index.get(name.toLowerCase(Locale.ROOT));
            }

            public static Set<String> getNames() {
                return index.keySet();
            }
        }
    }
}
