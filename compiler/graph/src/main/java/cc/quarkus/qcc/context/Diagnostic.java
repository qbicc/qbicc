package cc.quarkus.qcc.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A single diagnostic message.
 */
public final class Diagnostic {
    private final Diagnostic parent;
    private final Level level;
    private final String format;
    private final Object[] args;
    private final Location location;
    private final List<Diagnostic> children = new ArrayList<>(0);

    public Diagnostic(final Diagnostic parent, final Location location, final Level level, final String format, final Object... args) {
        this.parent = parent;
        this.level = level;
        this.format = format;
        this.args = args;
        this.location = location;
        if (parent != null) {
            synchronized (parent.children) {
                parent.children.add(this);
            }
        }
    }

    public Diagnostic getParent() {
        return parent;
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

    public List<Diagnostic> getChildren() {
        synchronized (children) {
            // copyOf doesn't short circuit this common case...
            return children.isEmpty() ? List.of() : List.copyOf(children);
        }
    }

    public void appendTo(Appendable output) throws IOException {
        if (output instanceof StringBuilder) {
            appendTo((StringBuilder) output);
        } else {
            output.append(appendTo(new StringBuilder()));
        }
    }

    public StringBuilder appendTo(StringBuilder builder) {
        if (location.hasLocation()) {
            location.appendBaseString(builder);
            builder.append(": ");
        }
        builder.append(level).append(": ");
        builder.append(getFormatted());
        builder.append('\n');
        if (location.hasMemberName()) {
            builder.append("    ");
            location.appendMemberString(builder);
            builder.append('\n');
        }
        if (location.hasClassName()) {
            builder.append("  ");
            location.appendLocationString(builder);
            builder.append('\n');
        }
        for (Diagnostic child : getChildren()) {
            child.appendTo(builder);
        }
        return builder;
    }

    public String toString() {
        return appendTo(new StringBuilder()).toString();
    }

    public enum Level {
        ERROR("error"),
        WARNING("warning"),
        NOTE("note"),
        INFO("info"),
        DEBUG("debug"),
        ;
        private final String str;

        Level(final String str) {
            this.str = str;
        }

        public String toString() {
            return str;
        }
    }
}
