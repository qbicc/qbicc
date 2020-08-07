package cc.quarkus.qcc.driver;

import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.context.Diagnostic;

/**
 * The main entry point (for now).
 */
public class Main {
    public static void main(String[] args) {
        // todo: pass configuration in to context
        final Context context = new Context(false);
        final Driver driver = Driver.builder().build();
        final boolean result = driver.execute();
        for (Diagnostic diagnostic : context.getDiagnostics()) {
            System.err.println(diagnostic.toString());
        }
        System.exit(result ? 0 : 1);
    }
}
