package org.qbicc.machine.arch;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.collections.api.map.primitive.ImmutableIntObjectMap;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;

/**
 * The possible CPU architectures.
 */
@SuppressWarnings("StaticInitializerReferencesSubClass")
public class Cpu extends PlatformComponent {
    // this has to be first to satisfy the class init gremlin
    private static final ClassValue<ImmutableIntObjectMap<Register>> REG_CV = new ClassValue<ImmutableIntObjectMap<Register>>() {
        @Override
        protected ImmutableIntObjectMap<Register> computeValue(Class<?> registers) {
            // type check
            registers.asSubclass(Enum.class).asSubclass(Register.class);
            Register[] enumConstants = (Register[]) registers.getEnumConstants();
            return IntObjectMaps.immutable.from(Arrays.asList(enumConstants), Register::getDwarfId, Functions.identity());
        }
    };

    public static final Cpu UNKNOWN = new Cpu(4, Register_None.class, "unknown");

    public static final Cpu X86_64 = new Cpu(8, Register_x86_64.class, "x86_64", "amd64");
    public static final Cpu X86 = new Cpu(4, Register_x86.class, "i686", "x86", "i386", "i486", "i586");
    public static final Cpu AARCH64 = new Cpu(8, Register_AArch64.class, "aarch64", "arm64");
    public static final ArmCpu ARMV4 = new ArmCpu(ArmCpuArch.V4);
    public static final ArmCpu ARMV4T = new ArmCpu(ArmCpuArch.V4T);
    public static final ArmCpu ARMV5TE = new ArmCpu(ArmCpuArch.V5TE);
    public static final ArmCpu ARMV6 = new ArmCpu(ArmCpuArch.V6);
    public static final ArmCpu ARMV6_M = new ArmCpu(ArmCpuArch.V6_M);
    public static final ArmCpu ARMV7 = new ArmCpu(ArmCpuArch.V7);
    public static final ArmCpu ARMV7_M = new ArmCpu(ArmCpuArch.V7_M);
    public static final ArmCpu ARMV7E_M = new ArmCpu(ArmCpuArch.V7E_M);
    public static final ArmCpu ARMV7_R = new ArmCpu(ArmCpuArch.V7_R);
    public static final Cpu ARM = new Cpu(4, Register_ARM.class, "arm", "armv7", "armv7hl");
    public static final Cpu PPC32 = new Cpu(4, /* todo */ Register_None.class, "ppc32");
    public static final Cpu PPC64 = new Cpu(8, /* todo */ Register_None.class, "ppc64");
    public static final Cpu WASM32 = new Cpu(4, Register_None.class, "wasm32", "wasm");

    private final int wordSize;
    private final ImmutableIntObjectMap<Register> registersByDwarfId;

    Cpu(final int wordSize, final Class<? extends Enum<?>> registers, final String name, final String... aliases) {
        super(name, aliases);
        registersByDwarfId = REG_CV.get(registers);
        this.wordSize = wordSize;
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
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }

    /**
     * Get the CPU register word size, in bytes.  This is used to determine how to
     * handle 64-bit integer values.
     *
     * @return the CPU word size
     */
    public int getCpuWordSize() {
        return wordSize;
    }

    public Register getRegisterById(int dwarfId) {
        Register register = registersByDwarfId.get(dwarfId);
        if (register == null) {
            throw new IllegalArgumentException("No register exists on " + this + " with ID " + dwarfId);
        }
        return register;
    }

    public sealed interface Register permits Register_AArch64, Register_ARM, Register_None, Register_x86, Register_x86_64 {
        /**
         * Get the human-readable name of this register.
         *
         * @return the register name
         */
        String getName();

        /**
         * Get the DWARF identifier for this register.
         *
         * @return the DWARF identifier
         */
        int getDwarfId();
    }
}
