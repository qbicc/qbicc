package cc.quarkus.qcc.driver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * The main entry point (for now).
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
                    System.err.println("Unrecognized argument \"" + arg + "\"");
                    System.exit(1);
                }
            } else if (mainClass == null) {
                mainClass = arg;
            } else {
                System.err.println("Extra argument \"" + arg + "\"");
                System.exit(1);
            }
        }
        if (mainClass == null) {
            System.err.println("No main class specified");
            System.exit(1);
        }
        // keep it simple to start with
        // todo: probe platform and initial type constraints
        builder.setTargetPlatform(Platform.HOST_PLATFORM);
        builder.setTypeSystem(TypeSystem.builder().build());
        builder.setMainClass(mainClass.replace('.', '/'));
        CompilationContext ctxt;
        boolean result;
        try (Driver driver = builder.build()) {
            ctxt = driver.getCompilationContext();
            result = driver.execute();
            for (Diagnostic diagnostic : driver.getDiagnostics()) {
                try {
                    diagnostic.appendTo(System.err);
                } catch (IOException e) {
                    // just give up
                    break;
                }
            }
        }
        int errors = ctxt.errors();
        int warnings = ctxt.warnings();
        if (errors > 0) {
            if (warnings > 0) {
                System.err.printf("Compilation failed with %d error(s) and %d warning(s)%n", Integer.valueOf(errors), Integer.valueOf(warnings));
            } else {
                System.err.printf("Compilation failed with %d error(s)%n", Integer.valueOf(errors));
            }
        } else if (warnings > 0) {
            System.err.printf("Compilation completed with %d warning(s)%n", Integer.valueOf(warnings));
        }
        System.exit(result ? 0 : 1);
    }
}
