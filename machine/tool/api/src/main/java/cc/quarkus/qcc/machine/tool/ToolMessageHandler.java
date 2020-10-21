package cc.quarkus.qcc.machine.tool;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Location;

/**
 *
 */
public interface ToolMessageHandler {
    ToolMessageHandler DISCARDING = new ToolMessageHandler() {
        public void handleMessage(final Tool tool, final Level level, final String file, final int line, final int column, final String message) {
        }
    };

    static ToolMessageHandler reporting(CompilationContext context) {
        return new ToolMessageHandler() {
            public void handleMessage(final Tool tool, final Level level, final String file, final int line, final int column, final String message) {
                switch (level) {
                    case ERROR:
                        context.error(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", tool.getProgramName(), message);
                        return;
                    case WARNING:
                        context.warning(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", tool.getProgramName(), message);
                        return;
                    case INFO:
                        context.note(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", tool.getProgramName(), message);
                        return;
                    default:
                        context.debug(Location.builder().setSourceFilePath(file).setLineNumber(line).build(), "%s: %s", tool.getProgramName(), message);
                        return;
                }
            }
        };
    }

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
