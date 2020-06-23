package cc.quarkus.qcc.driver;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.compiler.native_image.api.NativeImageGeneratorFactory;
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
                public String nativeImageGenerator() {
                    return "llvm-generic";
                }

                public String frontEnd() {
                    return "java";
                }
            };

            final ClassLoader classLoader = Main.class.getClassLoader();
            // initialize the JVM
            // todo: initialize the JVM
            Universe bootstrapLoader = new Universe(null);
            Universe.setRootUniverse(bootstrapLoader);
            // load the native image generator
            final NativeImageGeneratorFactory generatorFactory = NativeImageGeneratorFactory.getInstance(driverConfig.nativeImageGenerator(), classLoader);
            NativeImageGenerator generator = generatorFactory.createGenerator();
            generator.compile();
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
