package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ArmCpuArch extends PlatformComponent {
    public static final ArmCpuArch V4 = new ArmCpuArch("v4", ArmProfile.Classic);
    public static final ArmCpuArch V4T = new ArmCpuArch("v4t", ArmProfile.Classic, V4);
    public static final ArmCpuArch V5TE = new ArmCpuArch("v5te", ArmProfile.Classic, V4T);
    public static final ArmCpuArch V6 = new ArmCpuArch("v6", ArmProfile.Classic, V5TE);
    public static final ArmCpuArch V6_M = new ArmCpuArch("v6-m", ArmProfile.Microcontroller, V5TE);
    public static final ArmCpuArch V7 = new ArmCpuArch("v7", ArmProfile.Application, V6);
    public static final ArmCpuArch V7_M = new ArmCpuArch("v7-m", ArmProfile.Microcontroller, V6_M);
    public static final ArmCpuArch V7E_M = new ArmCpuArch("v7e-m", ArmProfile.Microcontroller, V7_M);
    public static final ArmCpuArch V7_R = new ArmCpuArch("v7-r", ArmProfile.RealTime, V6);

    private final Set<ArmCpuArch> incorporates;
    private final ArmProfile profile;

    ArmCpuArch(final String name, final ArmProfile profile, ArmCpuArch... incorporates) {
        super(name);
        this.profile = profile;
        this.incorporates = Set.of(incorporates);
    }

    public boolean incorporates(ArmCpuArch arch) {
        if (this == arch) {
            return true;
        }
        if (incorporates.contains(arch)) {
            return true;
        }
        for (ArmCpuArch incorporated : incorporates) {
            if (incorporated.incorporates(arch)) {
                return true;
            }
        }
        return false;
    }

    public String getName() {
        if (profile == ArmProfile.Application || profile == ArmProfile.Classic) {
            return super.getName();
        } else {
            return super.getName() + '-' + Character.toLowerCase(profile.getLetter());
        }
    }

    public ArmProfile getProfile() {
        return profile;
    }

    private static final Map<String, ArmCpuArch> index = Indexer.index(ArmCpuArch.class);

    public static ArmCpuArch forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
