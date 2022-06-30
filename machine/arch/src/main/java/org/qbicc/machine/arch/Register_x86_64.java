package org.qbicc.machine.arch;

/**
 * Registers for x86_64. Keep in order.
 */
public enum Register_x86_64 implements Cpu.Register  {
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

    fs_base("fs.base", 58),
    gs_base("gs.base", 59),

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

    private final String name;
    private final int dwarfId;

    Register_x86_64(String name, int dwarfId) {
        this.name = name == null ? name() : name;
        this.dwarfId = dwarfId == -1 ? ordinal() : dwarfId;
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    Register_x86_64(int dwarfId) {
        this(null, dwarfId);
    }

    Register_x86_64() {
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
