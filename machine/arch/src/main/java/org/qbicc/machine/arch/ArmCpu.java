package org.qbicc.machine.arch;

/**
 *
 */
public class ArmCpu extends Cpu {
    private final ArmCpuArch arch;

    ArmCpu(final ArmCpuArch arch) {
        super(4, Register_ARM.class, "arm");
        this.arch = arch;
    }

    public String getSimpleName() {
        return "arm";
    }

    public String getName() {
        return super.getName() + arch.getName();
    }

    public ArmCpuArch getArchitecture() {
        return arch;
    }
}
