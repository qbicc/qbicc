package cc.quarkus.qcc.diagnostic;

/**
 *
 */
public final class Diagnostic {
    private final Level level;
    private final String format;
    private final Object[] args;
    private final Location location;

    public Diagnostic(final Location location, final Level level, final String format, final Object... args) {
        this.level = level;
        this.format = format;
        this.args = args;
        this.location = location;
    }

    public Level getLevel() {
        return level;
    }

    public Location getLocation() {
        return location;
    }

    public String getFormatted() {
        return String.format(format, args);
    }

    public String toString() {
        final Location loc = this.location;
        return loc == null ? String.format("%s: %s", level, getFormatted())
                : String.format("%s: %s: %s", loc, level, getFormatted());
    }

    public enum Level {
        ERROR,
        WARNING,
        NOTE,
        DEBUG,
    }
}
