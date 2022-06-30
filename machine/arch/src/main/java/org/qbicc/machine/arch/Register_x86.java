package org.qbicc.machine.arch;

/**
 * Registers for x86 (i386). Keep in order.
 */
public enum Register_x86 implements Cpu.Register  {
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

    private final String name;
    private final int dwarfId;

    Register_x86(String name, int dwarfId) {
        this.name = name == null ? name() : name;
        this.dwarfId = dwarfId == -1 ? ordinal() : dwarfId;
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    Register_x86(int dwarfId) {
        this(null, dwarfId);
    }

    Register_x86() {
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
