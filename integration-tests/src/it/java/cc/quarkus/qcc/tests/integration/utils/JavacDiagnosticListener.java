package org.qbicc.tests.integration.utils;

import org.jboss.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;

public class JavacDiagnosticListener implements DiagnosticListener<JavaFileObject> {
    private final Logger logger;

    public JavacDiagnosticListener(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        String message = diagnostic.toString();
        switch (diagnostic.getKind()) {
            case ERROR:
                logger.error(message);
                break;
            case WARNING:
            case MANDATORY_WARNING:
                logger.warn(message);
                break;
            default:
                logger.info(message);
                break;
        }
    }
}
