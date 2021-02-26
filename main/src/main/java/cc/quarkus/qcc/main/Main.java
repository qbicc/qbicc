package cc.quarkus.qcc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.context.DiagnosticContext;
import cc.quarkus.qcc.driver.BaseDiagnosticContext;
import cc.quarkus.qcc.driver.BuilderStage;
import cc.quarkus.qcc.driver.Driver;
import cc.quarkus.qcc.driver.Phase;
import cc.quarkus.qcc.driver.plugin.DriverPlugin;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.probe.CProbe;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.plugin.constants.ConstantBasicBlockBuilder;
import cc.quarkus.qcc.plugin.conversion.CloneConversionBasicBlockBuilder;
import cc.quarkus.qcc.plugin.conversion.LLVMCompatibleBasicBlockBuilder;
import cc.quarkus.qcc.plugin.conversion.MethodCallFixupBasicBlockBuilder;
import cc.quarkus.qcc.plugin.conversion.NumericalConversionBasicBlockBuilder;
import cc.quarkus.qcc.plugin.correctness.RuntimeChecksBasicBlockBuilder;
import cc.quarkus.qcc.plugin.dispatch.DevirtualizingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.dispatch.DispatchTableEmitter;
import cc.quarkus.qcc.plugin.dispatch.VTableBuilder;
import cc.quarkus.qcc.plugin.dot.DotGenerator;
import cc.quarkus.qcc.plugin.gc.nogc.NoGcBasicBlockBuilder;
import cc.quarkus.qcc.plugin.gc.nogc.NoGcMultiNewArrayBasicBlockBuilder;
import cc.quarkus.qcc.plugin.gc.nogc.NoGcSetupHook;
import cc.quarkus.qcc.plugin.gc.nogc.NoGcTypeSystemConfigurator;
import cc.quarkus.qcc.plugin.instanceofcheckcast.InstanceOfCheckCastBasicBlockBuilder;
import cc.quarkus.qcc.plugin.instanceofcheckcast.RegisterHelperBasicBlockBuilder;
import cc.quarkus.qcc.plugin.instanceofcheckcast.SupersDisplayBuilder;
import cc.quarkus.qcc.plugin.intrinsics.IntrinsicBasicBlockBuilder;
import cc.quarkus.qcc.plugin.intrinsics.core.CoreIntrinsics;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.plugin.layout.ObjectAccessLoweringBuilder;
import cc.quarkus.qcc.plugin.linker.LinkStage;
import cc.quarkus.qcc.plugin.llvm.LLVMCompileStage;
import cc.quarkus.qcc.plugin.llvm.LLVMGenerator;
import cc.quarkus.qcc.plugin.lowering.InvocationLoweringBasicBlockBuilder;
import cc.quarkus.qcc.plugin.lowering.StaticFieldLoweringBasicBlockBuilder;
import cc.quarkus.qcc.plugin.lowering.ThrowExceptionHelper;
import cc.quarkus.qcc.plugin.lowering.ThrowLoweringBasicBlockBuilder;
import cc.quarkus.qcc.plugin.main_method.AddMainClassHook;
import cc.quarkus.qcc.plugin.main_method.MainMethod;
import cc.quarkus.qcc.plugin.native_.ConstTypeResolver;
import cc.quarkus.qcc.plugin.native_.ConstantDefiningBasicBlockBuilder;
import cc.quarkus.qcc.plugin.native_.ExternExportTypeBuilder;
import cc.quarkus.qcc.plugin.native_.FunctionTypeResolver;
import cc.quarkus.qcc.plugin.native_.NativeBasicBlockBuilder;
import cc.quarkus.qcc.plugin.native_.NativeTypeBuilder;
import cc.quarkus.qcc.plugin.native_.NativeTypeResolver;
import cc.quarkus.qcc.plugin.native_.PointerTypeResolver;
import cc.quarkus.qcc.plugin.objectmonitor.ObjectMonitorBasicBlockBuilder;
import cc.quarkus.qcc.plugin.opt.FenceAnalyzerVisitor;
import cc.quarkus.qcc.plugin.opt.GotoRemovingVisitor;
import cc.quarkus.qcc.plugin.opt.PhiOptimizerVisitor;
import cc.quarkus.qcc.plugin.opt.SimpleOptBasicBlockBuilder;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.plugin.reachability.ReachabilityBlockBuilder;
import cc.quarkus.qcc.plugin.threadlocal.ThreadLocalBasicBlockBuilder;
import cc.quarkus.qcc.plugin.threadlocal.ThreadLocalTypeBuilder;
import cc.quarkus.qcc.plugin.trycatch.LocalThrowHandlingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.trycatch.SynchronizedMethodBasicBlockBuilder;
import cc.quarkus.qcc.plugin.trycatch.ThrowValueBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.LowerVerificationBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.MemberResolvingBasicBlockBuilder;
import cc.quarkus.qcc.tool.llvm.LlvmToolChain;
import cc.quarkus.qcc.type.TypeSystem;
import io.smallrye.common.constraint.Assert;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.Logger;

/**
 * The main entry point, which can be constructed using a builder or directly invoked.
 */
public class Main implements Callable<DiagnosticContext> {
    private final List<Path> bootModulePath;
    private final Path outputPath;
    private final Consumer<Iterable<Diagnostic>> diagnosticsHandler;
    private final String mainClass;
    private final String gc;
    private final boolean isPie;

    Main(Builder builder) {
        bootModulePath = List.copyOf(builder.bootModulePath);
        outputPath = builder.outputPath;
        diagnosticsHandler = builder.diagnosticsHandler;
        // todo: this becomes optional
        mainClass = Assert.checkNotNullParam("builder.mainClass", builder.mainClass);
        gc = builder.gc;
        isPie = builder.isPie;
    }

    public DiagnosticContext call() {
        final BaseDiagnosticContext initialContext = new BaseDiagnosticContext();
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        boolean nogc = gc.equals("none");
        int errors = initialContext.errors();
        if (errors == 0) {
            builder.setOutputDirectory(outputPath);
            builder.addBootClassPathElements(bootModulePath);
            // first, probe the target platform
            Platform target = Platform.HOST_PLATFORM;
            builder.setTargetPlatform(target);
            Optional<ObjectFileProvider> optionalProvider = ObjectFileProvider.findProvider(target.getObjectType(), Main.class.getClassLoader());
            if (optionalProvider.isEmpty()) {
                initialContext.error("No object file provider found for %s", target.getObjectType());
            } else {
                ObjectFileProvider objectFileProvider = optionalProvider.get();
                Iterator<CToolChain> toolChains = CToolChain.findAllCToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
                if (! toolChains.hasNext()) {
                    initialContext.error("No working C compiler found");
                } else {
                    CToolChain toolChain = toolChains.next();
                    builder.setToolChain(toolChain);
                    // probe the basic system sizes
                    CProbe.Builder probeBuilder = CProbe.builder();
                    probeBuilder.include("<stdint.h>");
                    probeBuilder.include("<limits.h>");
                    // size and signedness of char
                    CProbe.Type char_t = CProbe.Type.builder().setName("char").build();
                    probeBuilder.probeType(char_t);
                    // int sizes
                    CProbe.Type int8_t = CProbe.Type.builder().setName("int8_t").build();
                    probeBuilder.probeType(int8_t);
                    CProbe.Type int16_t = CProbe.Type.builder().setName("int16_t").build();
                    probeBuilder.probeType(int16_t);
                    CProbe.Type int32_t = CProbe.Type.builder().setName("int32_t").build();
                    probeBuilder.probeType(int32_t);
                    CProbe.Type int64_t = CProbe.Type.builder().setName("int64_t").build();
                    probeBuilder.probeType(int64_t);
                    // float sizes
                    CProbe.Type float_t = CProbe.Type.builder().setName("float").build();
                    probeBuilder.probeType(float_t);
                    CProbe.Type double_t = CProbe.Type.builder().setName("double").build();
                    probeBuilder.probeType(double_t);
                    // bool
                    CProbe.Type _Bool = CProbe.Type.builder().setName("_Bool").build();
                    probeBuilder.probeType(_Bool);
                    // pointer
                    CProbe.Type void_p = CProbe.Type.builder().setName("void *").build();
                    probeBuilder.probeType(void_p);
                    // number of bits in char
                    probeBuilder.probeConstant("CHAR_BIT");
                    // execute
                    CProbe probe = probeBuilder.build();
                    try {
                        CProbe.Result probeResult = probe.run(toolChain, objectFileProvider, initialContext);
                        if (probeResult == null) {
                            initialContext.error("Type system probe compiler execution failed");
                        } else {
                            long charSize = probeResult.getTypeInfo(char_t).getSize();
                            if (charSize != 1) {
                                initialContext.error("Unexpected size of `char`: %d", Long.valueOf(charSize));
                            }
                            TypeSystem.Builder tsBuilder = TypeSystem.builder();
                            tsBuilder.setBoolSize((int) probeResult.getTypeInfo(_Bool).getSize());
                            tsBuilder.setBoolAlignment((int) probeResult.getTypeInfo(_Bool).getAlign());
                            tsBuilder.setByteBits(probeResult.getConstantInfo("CHAR_BIT").getValueAsInt());
                            tsBuilder.setInt8Size((int) probeResult.getTypeInfo(int8_t).getSize());
                            tsBuilder.setInt8Alignment((int) probeResult.getTypeInfo(int8_t).getAlign());
                            tsBuilder.setInt16Size((int) probeResult.getTypeInfo(int16_t).getSize());
                            tsBuilder.setInt16Alignment((int) probeResult.getTypeInfo(int16_t).getAlign());
                            tsBuilder.setInt32Size((int) probeResult.getTypeInfo(int32_t).getSize());
                            tsBuilder.setInt32Alignment((int) probeResult.getTypeInfo(int32_t).getAlign());
                            tsBuilder.setInt64Size((int) probeResult.getTypeInfo(int64_t).getSize());
                            tsBuilder.setInt64Alignment((int) probeResult.getTypeInfo(int64_t).getAlign());
                            tsBuilder.setFloat32Size((int) probeResult.getTypeInfo(float_t).getSize());
                            tsBuilder.setFloat32Alignment((int) probeResult.getTypeInfo(float_t).getAlign());
                            tsBuilder.setFloat64Size((int) probeResult.getTypeInfo(double_t).getSize());
                            tsBuilder.setFloat64Alignment((int) probeResult.getTypeInfo(double_t).getAlign());
                            tsBuilder.setPointerSize((int) probeResult.getTypeInfo(void_p).getSize());
                            tsBuilder.setPointerAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
                            // todo: function alignment probe
                            // for now, references == pointers
                            tsBuilder.setReferenceSize((int) probeResult.getTypeInfo(void_p).getSize());
                            tsBuilder.setReferenceAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
                            // for now, type IDs == int32
                            tsBuilder.setTypeIdSize((int) probeResult.getTypeInfo(int32_t).getSize());
                            tsBuilder.setTypeIdAlignment((int) probeResult.getTypeInfo(int32_t).getAlign());
                            if (nogc) {
                                new NoGcTypeSystemConfigurator().accept(tsBuilder);
                            }
                            builder.setTypeSystem(tsBuilder.build());
                            builder.setObjectFileProvider(objectFileProvider);
                            ServiceLoader<DriverPlugin> loader = ServiceLoader.load(DriverPlugin.class);
                            Iterator<DriverPlugin> iterator = loader.iterator();
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
                                Iterator<LlvmToolChain> llvmTools = LlvmToolChain.findAllLlvmToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
                                if (! llvmTools.hasNext()) {
                                    initialContext.error("No working LLVM toolchain found");
                                    errors = initialContext.errors();
                                } else {
                                    builder.setLlvmToolChain(llvmTools.next());
                                }
                            }
                            if (errors == 0) {
                                assert mainClass != null; // else errors would be != 0
                                // keep it simple to start with
                                builder.setMainClass(mainClass.replace('.', '/'));

                                builder.addTypeBuilderFactory(ExternExportTypeBuilder::new);
                                builder.addTypeBuilderFactory(NativeTypeBuilder::new);
                                builder.addTypeBuilderFactory(ThreadLocalTypeBuilder::new);

                                builder.addResolverFactory(ConstTypeResolver::new);
                                builder.addResolverFactory(FunctionTypeResolver::new);
                                builder.addResolverFactory(PointerTypeResolver::new);
                                builder.addResolverFactory(NativeTypeResolver::new);

                                builder.addPreHook(Phase.ADD, CoreIntrinsics::register);
                                builder.addPreHook(Phase.ADD, Layout::get);
                                builder.addPreHook(Phase.ADD, ThrowExceptionHelper::get);
                                builder.addPreHook(Phase.ADD, new AddMainClassHook());
                                if (nogc) {
                                    builder.addPreHook(Phase.ADD, new NoGcSetupHook());
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::new);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NoGcMultiNewArrayBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, CloneConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, LocalThrowHandlingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassLoadingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MemberResolvingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ThreadLocalBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantDefiningBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ThrowValueBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MethodCallFixupBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, SynchronizedMethodBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, RuntimeChecksBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, RegisterHelperBasicBlockBuilder::new);
                                builder.addPostHook(Phase.ADD, RTAInfo::clear);

                                builder.addCopyFactory(Phase.ANALYZE, GotoRemovingVisitor::new);
                                builder.addCopyFactory(Phase.ANALYZE, PhiOptimizerVisitor::new);
                                builder.addCopyFactory(Phase.ANALYZE, FenceAnalyzerVisitor::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.CORRECT, NumericalConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.ANALYZE, new VTableBuilder());
                                builder.addPostHook(Phase.ANALYZE, new SupersDisplayBuilder());

                                builder.addCopyFactory(Phase.LOWER, GotoRemovingVisitor::new);

                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ThrowLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, NoGcBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InvocationLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, StaticFieldLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectAccessLoweringBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InstanceOfCheckCastBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectMonitorBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LLVMCompatibleBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);

                                builder.addPreHook(Phase.GENERATE, new DispatchTableEmitter());
                                builder.addPreHook(Phase.GENERATE, new LLVMGenerator(isPie ? 2 : 0, isPie ? 2 : 0));

                                builder.addGenerateVisitor(DotGenerator.genPhase());

                                builder.addPostHook(Phase.GENERATE, new LLVMCompileStage(isPie));
                                builder.addPostHook(Phase.GENERATE, new LinkStage(isPie));


                                CompilationContext ctxt;
                                try (Driver driver = builder.build()) {
                                    ctxt = driver.getCompilationContext();
                                    MainMethod.get(ctxt).setMainClass(mainClass);
                                    driver.execute();
                                }
                            }
                        }
                    } catch (IOException e) {
                        initialContext.error(e, "Failed to probe system types from tool chain");
                    }
                }
            }
        }
        diagnosticsHandler.accept(initialContext.getDiagnostics());
        return initialContext;
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        Builder mainBuilder = builder();
        mainBuilder.setDiagnosticsHandler(diagnostics -> {
            for (Diagnostic diagnostic : diagnostics) {
                try {
                    diagnostic.appendTo(System.err);
                } catch (IOException e) {
                    // just give up
                    break;
                }
            }
        });
        final Iterator<String> argIter = List.of(args).iterator();
        String mainClass = null;
        Path outputPath = null;
        String gc = "none";

        // TODO Detect whether the system uses PIEs by default and match that if possible
        boolean isPie = false;

        while (argIter.hasNext()) {
            final String arg = argIter.next();
            if (arg.startsWith("-")) {
                if (arg.equals("--boot-module-path")) {
                    String[] path = argIter.next().split(Pattern.quote(File.pathSeparator));
                    for (String pathStr : path) {
                        if (! pathStr.isEmpty()) {
                            mainBuilder.addBootModulePath(Path.of(pathStr));
                        }
                    }
                } else if (arg.equals("--output-path") || arg.equals("-o")) {
                    outputPath = Path.of(argIter.next());
                } else if (arg.equals("--debug")) {
                    Logger.getLogger("").setLevel(Level.DEBUG);
                } else if (arg.equals("--debug-devirt")) {
                    Logger.getLogger("cc.quarkus.qcc.plugin.dispatch.devirt").setLevel(Level.DEBUG);
                } else if (arg.equals("--debug-vtables")) {
                    Logger.getLogger("cc.quarkus.qcc.plugin.dispatch.vtables").setLevel(Level.DEBUG);
                } else if (arg.equals("--debug-rta")) {
                    Logger.getLogger("cc.quarkus.qcc.plugin.reachability.rta").setLevel(Level.DEBUG);
                } else if (arg.equals("--debug-supers")) {
                    Logger.getLogger("cc.quarkus.qcc.plugin.instanceofcheckcast.supers").setLevel(Level.DEBUG);
                } else if (arg.startsWith("--gc=")) {
                    gc = arg.substring(5);
                } else if (arg.equals("--gc")) {
                    gc = argIter.next();
                } else if (arg.equals("--pie")) {
                    isPie = true;
                } else if (arg.equals("--no-pie")) {
                    isPie = false;
                } else {
                    System.err.printf("Unrecognized argument \"%s\"", arg);
                    System.exit(1);
                    return;
                }
            } else if (mainClass == null) {
                mainClass = arg;
            } else {
                System.err.printf("Extra argument \"%s\"", arg);
                System.exit(1);
                break;
            }
        }
        if (mainClass == null) {
            System.err.println("No main class specified");
            System.exit(1);
            return;
        }
        if (outputPath == null) {
            System.err.println("No output path specified");
            System.exit(1);
            return;
        }
        if (!gc.equals("none")) {
            System.err.printf("Unknown GC strategy \"%s\"%n", gc);
            System.exit(1);
        }
        mainBuilder.setMainClass(mainClass);
        mainBuilder.setOutputPath(outputPath);
        mainBuilder.setIsPie(isPie);
        Main main = mainBuilder.build();
        DiagnosticContext context = main.call();
        int errors = context.errors();
        int warnings = context.warnings();
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Path> bootModulePath = new ArrayList<>();
        private Path outputPath = Path.of(System.getProperty("java.io.tmpdir"), "qcc-output-" + Integer.toHexString(ThreadLocalRandom.current().nextInt()));
        private Consumer<Iterable<Diagnostic>> diagnosticsHandler = diagnostics -> {};
        private String mainClass;
        private String gc = "none";
        private boolean isPie = false;

        Builder() {}

        public Builder addBootModulePath(Path path) {
            Assert.checkNotNullParam("path", path);
            bootModulePath.add(path);
            return this;
        }

        public Builder addBootModulePaths(List<Path> paths) {
            Assert.checkNotNullParam("paths", paths);
            bootModulePath.addAll(paths);
            return this;
        }

        public Builder setOutputPath(Path outputPath) {
            Assert.checkNotNullParam("outputPath", outputPath);
            this.outputPath = outputPath;
            return this;
        }

        public Builder setDiagnosticsHandler(Consumer<Iterable<Diagnostic>> handler) {
            Assert.checkNotNullParam("handler", handler);
            diagnosticsHandler = handler;
            return this;
        }

        public Builder setMainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public Builder setGc(String gc) {
            this.gc = Assert.checkNotNullParam("gc", gc);
            return this;
        }

        public Builder setIsPie(boolean isPie) {
            this.isPie = isPie;
            return this;
        }

        public Main build() {
            return new Main(this);
        }
    }
}
