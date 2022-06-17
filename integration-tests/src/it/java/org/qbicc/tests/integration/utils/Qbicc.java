package org.qbicc.tests.integration.utils;

import java.nio.file.Path;

import org.jboss.logging.Logger;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.main.ClassPathEntry;
import org.qbicc.main.Main;

public class Qbicc {
    public static DiagnosticContext build(Path outputPath, Path nativeOutputPath, String mainClass, Logger logger) {
        return Main.builder().addAppPath(ClassPathEntry.of(outputPath))
            .setOutputPath(nativeOutputPath)
            .setDiagnosticsHandler(new QbiccDiagnosticLogger(logger))
            .setGraphGenConfig(new GraphGenConfig())
            .setMainClass(mainClass)
            .build()
            .call();
    }
}
