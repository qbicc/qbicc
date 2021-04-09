package org.qbicc.tests.integration.utils;

import java.util.function.Consumer;

import org.qbicc.context.Diagnostic;
import org.jboss.logging.Logger;

public class QbiccDiagnosticLogger implements Consumer<Iterable<Diagnostic>> {
    private final Logger logger;

    public QbiccDiagnosticLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void accept(Iterable<Diagnostic> diagnostics) {
        for (Diagnostic diagnostic : diagnostics) {
            String message = diagnostic.toString();
            switch (diagnostic.getLevel()) {
                case ERROR:
                    logger.error(message);
                case WARNING:
                    logger.warn(message);
                    break;
                case NOTE:
                case INFO:
                    logger.info(message);
                    break;
                case DEBUG:
                    logger.debug(message);
                    break;
            }
        }
    }

}
