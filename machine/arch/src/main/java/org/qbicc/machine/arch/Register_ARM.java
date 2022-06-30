package org.qbicc.machine.arch;

/**
 * Registers for 32-bit ARM. Keep in order.
 */
public enum Register_ARM implements Cpu.Register  {
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
    public static final Register_ARM pc = r15;
    public static final Register_ARM ip = r15;
    public static final Register_ARM lr = r14;

    private final String name;
    private final int dwarfId;

    Register_ARM(String name, int dwarfId) {
        this.name = name == null ? name() : name;
        this.dwarfId = dwarfId == -1 ? ordinal() : dwarfId;
    }

    @Override
    public String toString() {
        return "%" + name;
    }

    Register_ARM(int dwarfId) {
        this(null, dwarfId);
    }

    Register_ARM() {
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
