package cc.quarkus.qcc.machine.arch;

/**
 *
 */
public class ArmCpu extends Cpu {
    private final ArmCpuArch arch;

    ArmCpu(final ArmCpuArch arch) {
        super("arm");
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
