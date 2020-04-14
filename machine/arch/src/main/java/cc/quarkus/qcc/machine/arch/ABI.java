package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ABI extends PlatformComponent {
    public static final ABI UNKNOWN = new ABI("unknown");
    public static final ABI GNU = new ABI("gnu");
    // todo: subAbi?
    public static final ABI GNUEABI = new ABI("gnueabi");
    public static final ABI ELF = new ABI("elf");

    ABI(final String name) {
        super(name);
    }

    private static final Map<String, ABI> index = Indexer.index(ABI.class);

    public static ABI forName(String name) {
        return index.getOrDefault(name, UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
