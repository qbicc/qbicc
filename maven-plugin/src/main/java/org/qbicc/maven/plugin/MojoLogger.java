package org.qbicc.maven.plugin;

import java.text.MessageFormat;

import org.apache.maven.plugin.logging.Log;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

/**
 *
 */
final class MojoLogger extends Logger {
    static volatile Log pluginLog;
    
    MojoLogger(final String name) {
        super(name);
    }

    @Override
    protected void doLog(Level level, String loggerClassName, Object message, Object[] parameters, Throwable thrown) {
        final Log log = pluginLog;
        if (log != null) {
            doLogInternal(level, thrown, log, MessageFormat.format(String.valueOf(message), parameters));
        }
    }

    @Override
    protected void doLogf(Level level, String loggerClassName, String format, Object[] parameters, Throwable thrown) {
        final Log log = pluginLog;
        if (log != null) {
            doLogInternal(level, thrown, log, String.format(format, parameters));
        }
    }

    @Override
    public boolean isEnabled(Level level) {
        final Log log = pluginLog;
        switch (level) {
            case ERROR: return log == null || log.isErrorEnabled();
            case WARN: return log == null || log.isWarnEnabled();
            case INFO: return log == null || log.isInfoEnabled();
            default: return log == null || log.isDebugEnabled();
        }
    }

    private void doLogInternal(final Level level, final Throwable thrown, final Log log, final String formatted) {
        final String str = String.format("(%s) %s: %s", getName(), MDC.get("phase"), formatted).trim();
        switch (level) {
            case ERROR: {
                if (thrown != null) {
                    log.error(str, thrown);
                } else {
                    log.error(str);
                }
                break;
            }
            case WARN: {
                if (thrown != null) {
                    log.warn(str, thrown);
                } else {
                    log.warn(str);
                }
                break;
            }
            case INFO: {
                if (thrown != null) {
                    log.info(str, thrown);
                } else {
                    log.info(str);
                }
                break;
            }
            default: {
                if (thrown != null) {
                    log.debug(str, thrown);
                } else {
                    log.debug(str);
                }
                break;
            }
        }
    }
}
