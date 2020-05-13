package cc.quarkus.qcc.machine.arch;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public final class OS extends PlatformComponent {
    public static final OS UNKNOWN = new OS("unknown", ABI.UNKNOWN, ObjectType.UNKNOWN);

    public static final OS NONE = new OS("none", ABI.UNKNOWN, ObjectType.UNKNOWN);
    public static final OS LINUX = new OS("linux", ABI.GNU, ObjectType.ELF);
    public static final OS WIN32 = new OS("win32", ABI.WIN32, ObjectType.COFF, "windows", "windows32");
    public static final OS DARWIN = new OS("darwin", ABI.UNKNOWN, ObjectType.MACH_O);

    private final ABI defaultAbi;
    private final ObjectType defaultObjectType;

    OS(final String name, final ABI defaultAbi, final ObjectType defaultObjectType, final String... aliases) {
        super(name, aliases);
        this.defaultAbi = defaultAbi;
        this.defaultObjectType = defaultObjectType;
    }

    public ABI getDefaultAbi(final Cpu cpu) {
        // this is a bit of a hack; maybe revisit later
        return cpu instanceof ArmCpu && this == LINUX ? ABI.GNUEABI : defaultAbi;
    }

    public ObjectType getDefaultObjectType(final Cpu cpu) {
        return defaultObjectType;
    }

    private static final Map<String, OS> index = Indexer.index(OS.class);

    public static OS forName(String name) {
        return index.getOrDefault(name.toLowerCase(Locale.ROOT), UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
