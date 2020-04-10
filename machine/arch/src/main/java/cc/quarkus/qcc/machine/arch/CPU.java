package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 * The possible CPU architectures.
 */
public class CPU<S extends CpuSubArch> extends PlatformComponent {
    public static final CPU<NoSubArch> X86_64 = new CPU<>("x86_64");
    public static final CPU<NoSubArch> X86 = new CPU<>("x86");
    public static final CPU<NoSubArch> AARCH64 = new CPU<>("aarch64");
    public static final CPU<ArmSubArch> ARM = new CPU<>("arm");
    public static final CPU<NoSubArch> PPC32 = new CPU<>("ppc32");
    public static final CPU<NoSubArch> PPC64 = new CPU<>("ppc64");

    CPU(final String name) {
        super(name);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final Map<String, CPU<?>> index = (Map) Indexer.index(CPU.class);

    public static CPU<?> forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
