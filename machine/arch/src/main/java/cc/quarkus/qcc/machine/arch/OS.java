package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class OS extends PlatformComponent {
    public static final OS UNKNOWN = new OS("unknown", ABI.UNKNOWN);

    public static final OS NONE = new OS("none", ABI.UNKNOWN);
    public static final OS LINUX = new OS("linux", ABI.GNU);
    public static final OS WIN32 = new OS("win32", ABI.UNKNOWN);
    public static final OS DARWIN = new OS("darwin", ABI.UNKNOWN);
    public static final OS IOS = new OS("ios", ABI.UNKNOWN);
    public static final OS MACOS = new OS("macos", ABI.UNKNOWN);

    private final ABI defaultAbi;

    OS(final String name, final ABI defaultAbi) {
        super(name);
        this.defaultAbi = defaultAbi;
    }

    public ABI getDefaultAbi(final Cpu cpu) {
        // this is a bit of a hack; maybe revisit later
        return cpu instanceof ArmCpu && this == LINUX ? ABI.GNUEABI : defaultAbi;
    }

    private static final Map<String, OS> index = Indexer.index(OS.class);

    public static OS forName(String name) {
        return index.getOrDefault(name, UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
