package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 * The possible CPU architectures.
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public class Cpu extends PlatformComponent {
    public static final Cpu UNKNOWN = new Cpu("unknown");

    public static final Cpu X86_64 = new Cpu("x86_64", "amd64");
    public static final Cpu X86 = new Cpu("x86", "i386", "i486", "i586", "i686");
    public static final Cpu AARCH64 = new Cpu("aarch64", "arm64");
    public static final ArmCpu ARMV4 = new ArmCpu(ArmCpuArch.V4);
    public static final ArmCpu ARMV4T = new ArmCpu(ArmCpuArch.V4T);
    public static final ArmCpu ARMV5TE = new ArmCpu(ArmCpuArch.V5TE);
    public static final ArmCpu ARMV6 = new ArmCpu(ArmCpuArch.V6);
    public static final ArmCpu ARMV6_M = new ArmCpu(ArmCpuArch.V6_M);
    public static final ArmCpu ARMV7 = new ArmCpu(ArmCpuArch.V7);
    public static final ArmCpu ARMV7_M = new ArmCpu(ArmCpuArch.V7_M);
    public static final ArmCpu ARMV7E_M = new ArmCpu(ArmCpuArch.V7E_M);
    public static final ArmCpu ARMV7_R = new ArmCpu(ArmCpuArch.V7_R);
    public static final Cpu ARM = new Cpu("arm");
    public static final Cpu PPC32 = new Cpu("ppc32");
    public static final Cpu PPC64 = new Cpu("ppc64");

    Cpu(final String name, final String... aliases) {
        super(name);
    }

    private static final Map<String, Cpu> index = Indexer.index(Cpu.class);

    public String getSimpleName() {
        return getName();
    }

    public boolean incorporates(Cpu other) {
        // todo: fix this hack
        return this == other || this == ARM && other instanceof ArmCpu;
    }

    public static Cpu forName(String name) {
        return index.getOrDefault(name, UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
