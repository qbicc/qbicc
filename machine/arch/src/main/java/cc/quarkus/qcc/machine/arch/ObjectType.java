package cc.quarkus.qcc.machine.arch;

import java.util.Map;
import java.util.Set;

public final class ObjectType extends PlatformComponent {

    public static final ObjectType UNKNOWN = new ObjectType("unknown", "o");
    public static final ObjectType ELF = new ObjectType("elf", "o");
    public static final ObjectType MACH_O = new ObjectType("macho", "o");
    public static final ObjectType COFF = new ObjectType("coff", "obj");

    private final String objectSuffix;

    ObjectType(final String name, final String objectSuffix) {
        super(name);
        this.objectSuffix = objectSuffix;
    }

    public String objectSuffix() {
        return objectSuffix;
    }

    private static final Map<String, ObjectType> index = Indexer.index(ObjectType.class);

    public static ObjectType forName(String name) {
        return index.getOrDefault(name, UNKNOWN);
    }

    public static Set<String> getNames() {
        return index.keySet();
    }
}
