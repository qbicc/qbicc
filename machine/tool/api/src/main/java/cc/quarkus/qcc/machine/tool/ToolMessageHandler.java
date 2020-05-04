package cc.quarkus.qcc.machine.tool;

/**
 *
 */
public interface ToolMessageHandler {
    void handleMessage(Level level, String file, int line, int column, String message);

    enum Level {
        ERROR,
        WARNING,
        INFO,
        DEBUG,
        ;
    }
}
