package cc.quarkus.qcc.driver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import cc.quarkus.qcc.compiler.native_image.api.NativeImageGenerator;
import cc.quarkus.qcc.compiler.native_image.api.NativeImageGeneratorFactory;
import cc.quarkus.qcc.context.Context;
import cc.quarkus.qcc.graph.Type;
import cc.quarkus.qcc.type.descriptor.MethodIdentifier;
import cc.quarkus.qcc.type.descriptor.MethodTypeDescriptor;
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
            // ↓↓↓↓↓↓ TODO: temporary ↓↓↓↓↓↓
            defineInitialClass(bootstrapLoader, "java/lang/Object");
            defineInitialClass(bootstrapLoader, "java/lang/Class");
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
            defineInitialClass(bootstrapLoader, "hello/world/Main");
            generator.addEntryPoint(bootstrapLoader.findClass("hello/world/Main").verify().resolve().resolveMethod(MethodIdentifier.of("main", MethodTypeDescriptor.of(Type.S32))));
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
