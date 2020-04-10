package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class OS extends PlatformComponent {
    public static final OS NONE = new OS("none");
    public static final OS LINUX = new OS("linux");
    public static final OS WIN32 = new OS("win32");
    public static final OS DARWIN = new OS("darwin");
    public static final OS IOS = new OS("ios");
    public static final OS MACOS = new OS("macos");

    OS(final String name) {
        super(name);
    }

    private static final Map<String, OS> index = Indexer.index(OS.class);

    public static OS forName(String name) {
        return index.get(name);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
