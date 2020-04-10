package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ArmSubArch extends CpuSubArch {
    public static final ArmSubArch V7 = new ArmSubArch("v7");

    ArmSubArch(final String name) {
        super(name);
    }

    private static final Map<String, ArmSubArch> index = Indexer.index(ArmSubArch.class);

    public static ArmSubArch forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
