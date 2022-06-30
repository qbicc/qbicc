package org.qbicc.machine.arch;

/**
 * Registers for 64-bit ARM (AArch64). Keep in order.
 */
public enum Register_AArch64 implements Cpu.Register  {
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
    public static final Register_AArch64 ip = pc;

    private final String name;
    private final int dwarfId;

    Register_AArch64(String name, int dwarfId) {
        this.name = name == null ? name() : name;
        this.dwarfId = dwarfId == -1 ? ordinal() : dwarfId;
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    Register_AArch64(int dwarfId) {
        this(null, dwarfId);
    }

    Register_AArch64() {
        this(null, -1);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getDwarfId() {
        return dwarfId;
    }
}
