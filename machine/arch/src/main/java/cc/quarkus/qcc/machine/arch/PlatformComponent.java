package cc.quarkus.qcc.machine.arch;

/**
 *
 */
public abstract class PlatformComponent {
    private final String name;

    PlatformComponent(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return getName();
    }
}
