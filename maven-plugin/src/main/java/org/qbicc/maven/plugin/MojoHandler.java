package org.qbicc.maven.plugin;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.apache.maven.plugin.logging.Log;
import org.jboss.logmanager.formatters.PatternFormatter;

/**
 *
 */
final class MojoHandler extends Handler {
    private final Log log;

    MojoHandler(final Log log) {
        this.log = log;
        setFormatter(new PatternFormatter("(%c) %X{phase}: %m"));
    }

    @Override
    public void publish(LogRecord logRecord) {
        Level level = logRecord.getLevel();
        if (level.intValue() >= Level.SEVERE.intValue()) {
            log.error(getFormatter().format(logRecord));
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            log.warn(getFormatter().format(logRecord));
        } else if (level.intValue() >= Level.INFO.intValue()) {
            log.info(getFormatter().format(logRecord));
        } else {
            log.debug(getFormatter().format(logRecord));
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
