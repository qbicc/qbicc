package cc.quarkus.qcc.machine.arch;

import java.util.Set;

/**
 *
 */
public abstract class PlatformComponent {
    private final String name;
    private final Set<String> aliases;

    PlatformComponent(final String name, final String... aliases) {
        this.name = name;
        this.aliases = Set.of(aliases);
    }

    PlatformComponent(final String name) {
        this.name = name;
        this.aliases = Set.of();
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }

    public Set<String> getAliases() {
        return aliases;
    }
}
