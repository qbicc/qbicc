package cc.quarkus.qcc.machine.tool.process;

import java.io.IOException;

/**
 *
 */
public interface ProcessStatusChecker {
    void checkProcessStatus(Process finishedProcess) throws IOException;
}
