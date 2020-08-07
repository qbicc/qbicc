package cc.quarkus.qcc.driver;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.compiler.native_image.api.NativeImageGeneratorFactory;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.GraphFactory;
import cc.quarkus.qcc.interpreter.JavaVM;
import io.smallrye.common.constraint.Assert;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

/**
 * A simple driver to run all the stages of compilation.
 */
public class Driver {

    /*
        Reachability (Run Time)

        A class is reachable when any instance of that class can exist at run time.  This can happen only
        when either its constructor is reachable at run time, or when an instance of that class
        is reachable via the heap from an entry point.  The existence of a variable of a class type
        is not sufficient to cause the class to be reachable (the variable could be null-only) - there
        must be an actual value.

        A non-virtual method is reachable only when it can be directly called by another reachable method.

        A virtual method is reachable when it (or a method that the virtual method overrides) can be called
        by a reachable method and when its class is reachable.

        A static field is reachable when it can be accessed by a reachable method.
     */

    private final Context context;

    private Driver(final Builder builder) {
        this.context = new Context(false);
    }

    /**
     * Execute the compilation.
     *
     * @return {@code true} if compilation succeeded, {@code false} otherwise
     */
    public boolean execute() {
        Boolean result = context.run(() -> {
            final ClassLoader classLoader = Main.class.getClassLoader();

            Path javaBase = Maven.resolver().resolve("cc.quarkus.qcc.openjdk:java.base:jar:linux:11.0.8+8-1.0").withTransitivity().asSingleFile().toPath();
            // todo: map args to configurations
            DriverConfig driverConfig = new DriverConfig() {
                public String nativeImageGenerator() {
                    return "llvm-generic";
                }
            };
            // ▪ Load and initialize plugins
            List<Plugin> allPlugins = Plugin.findAllPlugins(List.of(classLoader), Set.of());
            List<GraphFactoryPlugin> graphFactoryPlugins = new ArrayList<>();
            for (Plugin plugin : allPlugins) {
                graphFactoryPlugins.addAll(plugin.getGraphFactoryPlugins());
            }
            graphFactoryPlugins.sort(Comparator.comparingInt(GraphFactoryPlugin::getPriority).thenComparing(a -> a.getClass().getName()).reversed());
            GraphFactory factory = GraphFactory.BASIC_FACTORY;
            for (GraphFactoryPlugin graphFactoryPlugin : graphFactoryPlugins) {
                factory = graphFactoryPlugin.construct(factory);
            }

            // ▫ Additive section ▫ Classes may be loaded and initialized
            // ▪ initialize the JVM
            JavaVM.Builder builder = JavaVM.builder();
            for (Plugin plugin : allPlugins) {
                for (Consumer<JavaVM.Builder> consumer : plugin.getJavaVMConfigurationPlugins()) {
                    consumer.accept(builder);
                }
            }
            builder.setGraphFactory(factory);
            JavaVM javaVM = builder.addBootstrapModules(List.of(javaBase)).build();
            // XXX
            // ▪ instantiate agent class loader(s)
            // XXX
            // ▪ initialize agent(s)
            // XXX
            // ▪ instantiate application class loader(s)
            // XXX
            // ▪ trace execution from entry points and collect reachable classes
            //   (initializing along the way)


            // ▪ terminate JVM and wait for all JVM threads to exit



            // load the native image generator
            final NativeImageGeneratorFactory generatorFactory = NativeImageGeneratorFactory.getInstance(driverConfig.nativeImageGenerator(), classLoader);
            NativeImageGenerator generator = generatorFactory.createGenerator();
            generator.compile();
            return Boolean.valueOf(context.errors() == 0);
        });
        return result.booleanValue();
    }

    /**
     * Construct a new builder.
     *
     * @return the new builder (not {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        final List<ClassLoader> searchLoaders = new ArrayList<>();
        final List<Consumer<JavaVM.Builder>> vmConfigurators = new ArrayList<>();

        Builder() {}

        /**
         * Add a class loader to the search path for plugins and implementations.
         *
         * @param loader the class loader (must not be {@code null})
         * @return this builder
         */
        public Builder addSearchClassLoader(ClassLoader loader) {
            searchLoaders.add(Assert.checkNotNullParam("loader", loader));
            return this;
        }

        public Builder addVMConfigurator(Consumer<JavaVM.Builder> configurator) {
            vmConfigurators.add(Assert.checkNotNullParam("configurator", configurator));
            return this;
        }

        public Driver build() {
            return new Driver(this);
        }
    }
}
