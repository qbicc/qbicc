package cc.quarkus.qcc.driver;

import java.io.File;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * The main entry point (for now).
 */
public class Main {
    public static void main(String[] args) {
        final Driver.Builder builder = Driver.builder();
        final Iterator<String> argIter = List.of(args).iterator();
        String entryClass = null;
        String entryMethod = null;
        while (argIter.hasNext()) {
            final String arg = argIter.next();
            if (arg.startsWith("-")) {
                if (arg.equals("--boot-module-path")) {
                    String[] path = argIter.next().split(Pattern.quote(File.pathSeparator));
                    for (String pathStr : path) {
                        if (! pathStr.isEmpty()) {
                            builder.addBootModule(Path.of(pathStr));
                        }
                    }
                } else {
                    System.err.println("Unrecognized argument \"" + arg + "\"");
                    System.exit(1);
                }
            } else if (entryClass == null) {
                entryClass = arg;
            } else if (entryMethod == null) {
                entryMethod = arg;
            } else {
                System.err.println("Extra argument \"" + arg + "\"");
                System.exit(1);
            }
        }
        if (entryClass == null) {
            System.err.println("No entry point class specified");
            System.exit(1);
        }
        if (entryMethod == null) {
            System.err.println("No entry point method specified");
            System.exit(1);
        }
        // keep it simple to start with
        // todo: probe platform and initial type constraints
        builder.setTargetPlatform(Platform.HOST_PLATFORM);
        builder.setTypeSystem(TypeSystem.builder().build());
        final Driver driver = builder.build();
        final boolean result = driver.execute();
        for (Diagnostic diagnostic : driver.getDiagnostics()) {
            System.err.println(diagnostic.toString());
        }
        System.exit(result ? 0 : 1);
    }
}
