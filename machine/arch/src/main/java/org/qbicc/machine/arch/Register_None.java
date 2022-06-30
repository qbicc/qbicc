package org.qbicc.machine.arch;

/**
 *
 */
public enum Register_None implements Cpu.Register {
    ;

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getDwarfId() {
        return 0;
    }
}
