package cc.quarkus.qcc.driver;

import cc.quarkus.qcc.compiler.backend.api.BackEnd;
import cc.quarkus.qcc.compiler.frontend.api.FrontEnd;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;

/**
 * A simple driver to run all the stages of compilation.
 */
public class Driver {

    private final Context context;

    private Driver(final Context context) {
        this.context = context;
    }

    /**
     * Execute the compilation.
     *
     * @return {@code true} if compilation succeeded, {@code false} otherwise
     */
    public boolean execute() {
        Boolean result = context.run(() -> {
            // todo: map args to configurations
            DriverConfig driverConfig = new DriverConfig() {
                public String backEnd() {
                    return "llvm-generic";
                }

                public String frontEnd() {
                    return "java";
                }
            };

            final ClassLoader classLoader = Main.class.getClassLoader();
            final FrontEnd frontEnd = FrontEnd.getInstance(driverConfig.frontEnd(), classLoader);
            final BackEnd backEnd = BackEnd.getInstance(driverConfig.backEnd(), classLoader);
            // now do the first stage of compilation
            final Universe universe = frontEnd.compile();
            if (context.errors() != 0) {
                return Boolean.FALSE;
            }
            // that was easy; now run it through the back end
            backEnd.compile(universe);
            return Boolean.valueOf(context.errors() == 0);
        });
        return result.booleanValue();
    }

    /**
     * Create a new driver instance.
     *
     * @param context the context (must not be {@code null})
     * @return the driver instance
     */
    public static Driver create(final Context context) {
        Assert.checkNotNullParam("context", context);
        return new Driver(context);
    }
}
