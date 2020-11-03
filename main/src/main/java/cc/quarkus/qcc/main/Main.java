package cc.quarkus.qcc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.driver.BaseDiagnosticContext;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.driver.plugin.DriverPlugin;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * The main entry point.
 */
public class Main {
    public static void main(String[] args) {
        final BaseDiagnosticContext initialContext = new BaseDiagnosticContext();
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        final Iterator<String> argIter = List.of(args).iterator();
        String mainClass = null;
        while (argIter.hasNext()) {
            final String arg = argIter.next();
            if (arg.startsWith("-")) {
                if (arg.equals("--boot-module-path")) {
                    String[] path = argIter.next().split(Pattern.quote(File.pathSeparator));
                    for (String pathStr : path) {
                        if (! pathStr.isEmpty()) {
                            builder.addBootClassPathElement(Path.of(pathStr));
                        }
                    }
                } else {
                    initialContext.error("Unrecognized argument \"%s\"", arg);
                    break;
                }
            } else if (mainClass == null) {
                mainClass = arg;
            } else {
                initialContext.error("Extra argument \"%s\"", arg);
                break;
            }
        }
        if (mainClass == null) {
            initialContext.error("No main class specified");
        }
        int errors = initialContext.errors();
        if (errors == 0) {
            ServiceLoader<DriverPlugin> loader = ServiceLoader.load(DriverPlugin.class);
            Iterator<DriverPlugin> iterator = loader.iterator();
            // todo: probe platform and initial type constraints
            builder.setTargetPlatform(Platform.HOST_PLATFORM);
            builder.setTypeSystem(TypeSystem.builder().build());
            for (;;) try {
                if (! iterator.hasNext()) {
                    break;
                }
                DriverPlugin plugin = iterator.next();
                plugin.accept(builder);
            } catch (ServiceConfigurationError error) {
                initialContext.error(error, "Failed to load plugin");
            }
            errors = initialContext.errors();
            if (errors == 0) {
                assert mainClass != null; // else errors would be != 0
                // keep it simple to start with
                builder.setMainClass(mainClass.replace('.', '/'));
                CompilationContext ctxt;
                boolean result;
                try (Driver driver = builder.build()) {
                    ctxt = driver.getCompilationContext();
                    driver.execute();
                }
                errors = ctxt.errors();
            }
        }
        for (Diagnostic diagnostic : initialContext.getDiagnostics()) {
            try {
                diagnostic.appendTo(System.err);
            } catch (IOException e) {
                // just give up
                break;
            }
        }
        int warnings = initialContext.warnings();
        if (errors > 0) {
            if (warnings > 0) {
                System.err.printf("Compilation failed with %d error(s) and %d warning(s)%n", Integer.valueOf(errors), Integer.valueOf(warnings));
            } else {
                System.err.printf("Compilation failed with %d error(s)%n", Integer.valueOf(errors));
            }
        } else if (warnings > 0) {
            System.err.printf("Compilation completed with %d warning(s)%n", Integer.valueOf(warnings));
        }
        System.exit(errors == 0 ? 0 : 1);
    }
}
