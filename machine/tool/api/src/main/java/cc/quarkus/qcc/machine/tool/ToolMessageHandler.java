package cc.quarkus.qcc.machine.tool;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.context.Location;

/**
 *
 */
public interface ToolMessageHandler {
    ToolMessageHandler DISCARDING = new ToolMessageHandler() {
        public void handleMessage(final Tool tool, final Level level, final String file, final int line, final int column, final String message) {
        }
    };

    ToolMessageHandler REPORTING = new ToolMessageHandler() {
        public void handleMessage(final Tool tool, final Level level, final String file, final int line, final int column, final String message) {
            switch (level) {
                case ERROR:
                    Context.error(new Location(file, line), "%s: %s", tool.getProgramName(), message);
                    return;
                case WARNING:
                    Context.warning(new Location(file, line), "%s: %s", tool.getProgramName(), message);
                    return;
                case INFO:
                    Context.note(new Location(file, line), "%s: %s", tool.getProgramName(), message);
                    return;
                default:
                    Context.debug(new Location(file, line), "%s: %s", tool.getProgramName(), message);
                    return;
            }
        }
    };

    void handleMessage(Tool tool, Level level, String file, int line, int column, String message);

    enum Level {
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        ;

        public Level max(Level other) {
            return other.ordinal() >= ordinal() ? this : other;
        }

        public Level min(Level other) {
            return other.ordinal() <= ordinal() ? this : other;
        }
    }
}
