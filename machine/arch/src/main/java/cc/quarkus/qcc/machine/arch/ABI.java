package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class ABI extends PlatformComponent {
    public static final ABI GNU = new ABI("gnu");

    ABI(final String name) {
        super(name);
    }

    private static final Map<String, ABI> index = Indexer.index(ABI.class);

    public static ABI forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
