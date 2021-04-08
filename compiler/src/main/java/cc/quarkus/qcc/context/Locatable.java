package cc.quarkus.qcc.context;

/**
 * A context or element which can yield a location for diagnostic reporting.
 */
public interface Locatable {
    /**
     * Get the location to use for diagnostic reporting.
     *
     * @return the location (must not be {@code null})
     */
    Location getLocation();

    Locatable EMPTY = () -> Location.NO_LOC;
}
