package cc.quarkus.qcc.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.compiler.native_image.api.NativeImageGeneratorFactory;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.interpreter.JavaVM;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
import cc.quarkus.qcc.type.universe.Universe;
import io.smallrye.common.constraint.Assert;

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
            final ClassLoader classLoader = Main.class.getClassLoader();

            // todo: this is just temporary... sorry :(
            Path javaBase = Path.of(System.getProperty("user.home"), ".m2/repository/cc/quarkus/qcc/openjdk/java.base/11.0.8+8-1.0/java.base-11.0.8+8-1.0-linux.jar");
            // todo: map args to configurations
            DriverConfig driverConfig = new DriverConfig() {
                public String nativeImageGenerator() {
                    return "llvm-generic";
                }

                public List<Path> bootstrapModules() {
                    return List.of(javaBase);
                }
            };
            // ▪ Load and initialize plugins
            List<Plugin> allPlugins = Plugin.findAllPlugins(List.of(classLoader), Set.of());
            List<GraphFactoryPlugin> graphFactoryPlugins = new ArrayList<>();
            for (Plugin plugin : allPlugins) {
                graphFactoryPlugins.addAll(plugin.getGraphFactoryPlugins());
            }
            graphFactoryPlugins.sort(Comparator.comparingInt(GraphFactoryPlugin::getPriority).thenComparing(a -> a.getClass().getName()));


            // ▫ Additive section ▫ Classes may be loaded and initialized
            // ▪ set up bootstrap class dictionary
            Universe bootstrapLoader = new Universe();
            Universe.setRootUniverse(bootstrapLoader);
            // ▪ initialize the JVM
            JavaVM javaVM = JavaVM.create(bootstrapLoader, driverConfig.bootstrapModules());
            // XXX
            // ▪ initialize bootstrap classes
            // XXX
            // ▪ instantiate bootstrap class loader with dictionary
            // XXX
            // ▪ instantiate JDK class loaders with dictionaries
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
            // ↓↓↓↓↓↓ TODO: temporary ↓↓↓↓↓↓
            defineInitialClass(bootstrapLoader, "java/io/Serializable");
            defineInitialClass(bootstrapLoader, "java/lang/reflect/GenericDeclaration");
            defineInitialClass(bootstrapLoader, "java/lang/reflect/AnnotatedElement");
            defineInitialClass(bootstrapLoader, "java/lang/String");
            defineInitialClass(bootstrapLoader, "java/lang/reflect/Type");
            defineInitialClass(bootstrapLoader, "java/lang/Comparable");
            defineInitialClass(bootstrapLoader, "java/lang/CharSequence");
            defineInitialClass(bootstrapLoader, "java/lang/System");
            defineInitialClass(bootstrapLoader, "java/io/Console");
            defineInitialClass(bootstrapLoader, "java/io/Flushable");
            defineInitialClass(bootstrapLoader, "java/io/InputStream");
            defineInitialClass(bootstrapLoader, "java/io/Closeable");
            defineInitialClass(bootstrapLoader, "java/lang/AutoCloseable");
            defineInitialClass(bootstrapLoader, "java/io/FilterOutputStream");
            defineInitialClass(bootstrapLoader, "java/io/PrintStream");
            defineInitialClass(bootstrapLoader, "java/io/OutputStream");
            defineInitialClass(bootstrapLoader, "java/lang/Appendable");
            defineInitialClass(bootstrapLoader, "java/lang/SecurityManager");
            defineInitialClass(bootstrapLoader, "java/util/Properties");
            defineInitialClass(bootstrapLoader, "java/util/Hashtable");
            defineInitialClass(bootstrapLoader, "java/util/Dictionary");
            defineInitialClass(bootstrapLoader, "java/util/Map");
            defineInitialClass(bootstrapLoader, "java/lang/Cloneable");
            defineInitialClass(bootstrapLoader, "java/lang/ModuleLayer");
            defineInitialClass(bootstrapLoader, "java/nio/channels/Channel");
            defineInitialClass(bootstrapLoader, "java/lang/System$Logger");
            defineInitialClass(bootstrapLoader, "java/util/ResourceBundle");
            defineInitialClass(bootstrapLoader, "java/io/FileOutputStream");
            defineInitialClass(bootstrapLoader, "java/lang/Throwable");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/SharedSecrets");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaIOAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/Unsafe");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaUtilJarAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaLangAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaLangModuleAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaLangInvokeAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaLangRefAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetInetAddressAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetHttpCookieAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetSocketAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetUriAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetURLAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNetURLClassLoaderAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaNioAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaIOFileDescriptorAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaIOFilePermissionAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaSecurityAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaUtilZipFileAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaUtilResourceBundleAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaAWTAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaAWTFontAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaBeansAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaObjectInputStreamAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaObjectInputFilterAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaIORandomAccessFileAccess");
            defineInitialClass(bootstrapLoader, "jdk/internal/misc/JavaxCryptoSealedObjectAccess");
            defineInitialClass(bootstrapLoader, "java/nio/charset/Charset");
            defineInitialClass(bootstrapLoader, "hello/world/Main");
            generator.addEntryPoint(bootstrapLoader.findClass("hello/world/Main").verify().resolve().resolveMethod(MethodIdentifier.of("main", MethodTypeDescriptor.of(Type.S32, Type.S32, Type.S32))));
            // ↑↑↑↑↑↑ TODO: temporary ↑↑↑↑↑↑
            generator.compile();
            return Boolean.valueOf(context.errors() == 0);
        });
        return result.booleanValue();
    }


    private static void defineInitialClass(Universe universe, String className) {
        InputStream stream = Driver.class.getClassLoader().getResourceAsStream(className + ".class");
        if (stream == null) {
            try {
                stream = Files.newInputStream(Path.of(System.getProperty("qcc.compile.class-path", "/tmp"), className + ".class"));
            } catch (IOException e) {
                throw new IllegalStateException("Missing class " + className);
            }
        }
        try {
            universe.defineClass(className, ByteBuffer.wrap(stream.readAllBytes()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
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
