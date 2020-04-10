package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class Vendor extends PlatformComponent {
    public static final Vendor APPLE = new Vendor("apple");
    public static final Vendor IBM = new Vendor("ibm");
    public static final Vendor PC = new Vendor("pc");

    Vendor(final String name) {
        super(name);
    }

    private static final Map<String, Vendor> index = Indexer.index(Vendor.class);

    public static Vendor forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
