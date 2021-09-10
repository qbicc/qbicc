package org.qbicc.main;

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

import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.driver.BaseDiagnosticContext;
import org.qbicc.driver.BuilderStage;
import org.qbicc.driver.Driver;
import org.qbicc.driver.ElementBodyCopier;
import org.qbicc.driver.ElementBodyCreator;
import org.qbicc.driver.ElementVisitorAdapter;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.driver.Phase;
import org.qbicc.driver.plugin.DriverPlugin;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.probe.CProbe;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.constants.ConstantBasicBlockBuilder;
import org.qbicc.plugin.conversion.LLVMCompatibleBasicBlockBuilder;
import org.qbicc.plugin.conversion.MethodCallFixupBasicBlockBuilder;
import org.qbicc.plugin.conversion.NumericalConversionBasicBlockBuilder;
import org.qbicc.plugin.core.CoreAnnotationTypeBuilder;
import org.qbicc.plugin.correctness.RuntimeChecksBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DevirtualizingBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DispatchTableEmitter;
import org.qbicc.plugin.dispatch.DispatchTableBuilder;
import org.qbicc.plugin.dot.DotGenerator;
import org.qbicc.plugin.gc.nogc.NoGcBasicBlockBuilder;
import org.qbicc.plugin.gc.nogc.NoGcMultiNewArrayBasicBlockBuilder;
import org.qbicc.plugin.gc.nogc.NoGcSetupHook;
import org.qbicc.plugin.gc.nogc.NoGcTypeSystemConfigurator;
import org.qbicc.plugin.instanceofcheckcast.ClassInitializerRegister;
import org.qbicc.plugin.instanceofcheckcast.InstanceOfCheckCastBasicBlockBuilder;
import org.qbicc.plugin.instanceofcheckcast.LowerClassInitCheckBlockBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayEmitter;
import org.qbicc.plugin.intrinsics.IntrinsicBasicBlockBuilder;
import org.qbicc.plugin.intrinsics.core.CoreIntrinsics;
import org.qbicc.plugin.llvm.LLVMDefaultModuleCompileStage;
import org.qbicc.plugin.lowering.LocalVariableFindingBasicBlockBuilder;
import org.qbicc.plugin.lowering.LocalVariableLoweringBasicBlockBuilder;
import org.qbicc.plugin.layout.ObjectAccessLoweringBuilder;
import org.qbicc.plugin.linker.LinkStage;
import org.qbicc.plugin.llvm.LLVMCompileStage;
import org.qbicc.plugin.llvm.LLVMGenerator;
import org.qbicc.plugin.lowering.BooleanAccessBasicBlockBuilder;
import org.qbicc.plugin.lowering.FunctionLoweringElementHandler;
import org.qbicc.plugin.lowering.InvocationLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.StaticFieldLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.ThrowExceptionHelper;
import org.qbicc.plugin.lowering.ThrowLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.VMHelpersSetupHook;
import org.qbicc.plugin.main_method.AddMainClassHook;
import org.qbicc.plugin.main_method.MainMethod;
import org.qbicc.plugin.methodinfo.MethodDataEmitter;
import org.qbicc.plugin.native_.ConstTypeResolver;
import org.qbicc.plugin.native_.ConstantDefiningBasicBlockBuilder;
import org.qbicc.plugin.native_.ExternExportTypeBuilder;
import org.qbicc.plugin.native_.FunctionTypeResolver;
import org.qbicc.plugin.native_.InternalNativeTypeResolver;
import org.qbicc.plugin.native_.NativeBasicBlockBuilder;
import org.qbicc.plugin.native_.NativeBindingBasicBlockBuilder;
import org.qbicc.plugin.native_.NativeBindingTypeBuilder;
import org.qbicc.plugin.native_.NativeTypeBuilder;
import org.qbicc.plugin.native_.NativeTypeResolver;
import org.qbicc.plugin.native_.PointerBasicBlockBuilder;
import org.qbicc.plugin.native_.PointerTypeResolver;
import org.qbicc.plugin.native_.StructMemberAccessBasicBlockBuilder;
import org.qbicc.plugin.objectmonitor.ObjectMonitorBasicBlockBuilder;
import org.qbicc.plugin.opt.GotoRemovingVisitor;
import org.qbicc.plugin.opt.LocalMemoryTrackingBasicBlockBuilder;
import org.qbicc.plugin.opt.InliningBasicBlockBuilder;
import org.qbicc.plugin.opt.PhiOptimizerVisitor;
import org.qbicc.plugin.opt.SimpleOptBasicBlockBuilder;
import org.qbicc.plugin.opt.ea.EscapeAnalysisIntraMethodBuilder;
import org.qbicc.plugin.opt.ea.ConnectionGraphDotGenerator;
import org.qbicc.plugin.opt.ea.EscapeAnalysisOptimizeVisitor;
import org.qbicc.plugin.opt.ea.EscapeAnalysisState;
import org.qbicc.plugin.reachability.RTAInfo;
import org.qbicc.plugin.reachability.ReachabilityBlockBuilder;
import org.qbicc.plugin.serialization.ClassObjectSerializer;
import org.qbicc.plugin.serialization.ObjectLiteralSerializingVisitor;
import org.qbicc.plugin.stringpool.StringPoolEmitter;
import org.qbicc.plugin.threadlocal.ThreadLocalBasicBlockBuilder;
import org.qbicc.plugin.threadlocal.ThreadLocalTypeBuilder;
import org.qbicc.plugin.trycatch.LocalThrowHandlingBasicBlockBuilder;
import org.qbicc.plugin.trycatch.SynchronizedMethodBasicBlockBuilder;
import org.qbicc.plugin.trycatch.ThrowValueBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassInitializingBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder;
import org.qbicc.plugin.verification.LowerVerificationBasicBlockBuilder;
import org.qbicc.plugin.verification.MemberResolvingBasicBlockBuilder;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.type.TypeSystem;
import io.smallrye.common.constraint.Assert;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.Logger;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

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
    private final GraphGenConfig graphGenConfig;
    private final boolean optMemoryTracking;
    private final boolean optPhis;
    private final boolean optGotos;
    private final boolean optInlining;
    private final Platform platform;

    Main(Builder builder) {
        bootModulePath = List.copyOf(builder.bootModulePath);
        outputPath = builder.outputPath;
        diagnosticsHandler = builder.diagnosticsHandler;
        // todo: this becomes optional
        mainClass = Assert.checkNotNullParam("builder.mainClass", builder.mainClass);
        gc = builder.gc;
        isPie = builder.isPie;
        graphGenConfig = builder.graphGenConfig;
        optMemoryTracking = builder.optMemoryTracking;
        optInlining = builder.optInlining;
        optPhis = builder.optPhis;
        optGotos = builder.optGotos;
        platform = builder.platform;
    }

    public DiagnosticContext call() {
        BaseDiagnosticContext ctxt = new BaseDiagnosticContext();
        try {
            call0(ctxt);
        } catch (Throwable t) {
            t.printStackTrace(System.err);
            ctxt.error(t, "Compilation failed due to an exception");
        }
        diagnosticsHandler.accept(ctxt.getDiagnostics());
        return ctxt;
    }

    DiagnosticContext call0(BaseDiagnosticContext initialContext) {
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        boolean nogc = gc.equals("none");
        int errors = initialContext.errors();
        if (errors == 0) {
            builder.setOutputDirectory(outputPath);
            builder.addBootClassPathElements(bootModulePath);
            // first, probe the target platform
            Platform target = platform;
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
                    // max alignment
                    CProbe.Type max_align_t = CProbe.Type.builder().setName("max_align_t").build();
                    probeBuilder.probeType(max_align_t);
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
                            tsBuilder.setMaxAlignment((int) probeResult.getTypeInfo(max_align_t).getAlign());
                            // todo: function alignment probe
                            // for now, references == pointers
                            tsBuilder.setReferenceSize((int) probeResult.getTypeInfo(void_p).getSize());
                            tsBuilder.setReferenceAlignment((int) probeResult.getTypeInfo(void_p).getAlign());
                            // for now, type IDs == int32
                            tsBuilder.setTypeIdSize((int) probeResult.getTypeInfo(int32_t).getSize());
                            tsBuilder.setTypeIdAlignment((int) probeResult.getTypeInfo(int32_t).getAlign());
                            tsBuilder.setEndianness(probeResult.getByteOrder());
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
                                LlvmToolChain llvmToolChain = null;
                                while (llvmTools.hasNext()) {
                                    llvmToolChain = llvmTools.next();
                                    if (llvmToolChain.compareVersionTo("12") >= 0) {
                                        break;
                                    }
                                    llvmToolChain = null;
                                }
                                if (llvmToolChain == null) {
                                    initialContext.error("No working LLVM toolchain found");
                                    errors = initialContext.errors();
                                } else {
                                    builder.setLlvmToolChain(llvmToolChain);
                                }
                            }
                            if (errors == 0) {
                                assert mainClass != null; // else errors would be != 0
                                // keep it simple to start with
                                builder.setMainClass(mainClass.replace('.', '/'));

                                builder.addTypeBuilderFactory(ExternExportTypeBuilder::new);
                                builder.addTypeBuilderFactory(NativeTypeBuilder::new);
                                builder.addTypeBuilderFactory(NativeBindingTypeBuilder::new);
                                builder.addTypeBuilderFactory(ThreadLocalTypeBuilder::new);
                                builder.addTypeBuilderFactory(CoreAnnotationTypeBuilder::new);

                                builder.addResolverFactory(ConstTypeResolver::new);
                                builder.addResolverFactory(FunctionTypeResolver::new);
                                builder.addResolverFactory(PointerTypeResolver::new);
                                builder.addResolverFactory(InternalNativeTypeResolver::new);
                                builder.addResolverFactory(NativeTypeResolver::new);

                                builder.addPreHook(Phase.ADD, CoreIntrinsics::register);
                                builder.addPreHook(Phase.ADD, CoreClasses::get);
                                builder.addPreHook(Phase.ADD, ThrowExceptionHelper::get);
                                builder.addPreHook(Phase.ADD, new VMHelpersSetupHook());
                                builder.addPreHook(Phase.ADD, new AddMainClassHook());
                                if (nogc) {
                                    builder.addPreHook(Phase.ADD, new NoGcSetupHook());
                                }
                                builder.addPreHook(Phase.ADD, RTAInfo::forceCoreClassesReachable);
                                builder.addElementHandler(Phase.ADD, new ElementBodyCreator());
                                builder.addElementHandler(Phase.ADD, new ElementVisitorAdapter(new DotGenerator(Phase.ADD, graphGenConfig)));
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForAddPhase);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NoGcMultiNewArrayBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassLoadingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MemberResolvingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBindingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, StructMemberAccessBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, PointerBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ThreadLocalBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassInitializingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantDefiningBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ThrowValueBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MethodCallFixupBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, SynchronizedMethodBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, RuntimeChecksBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, LocalThrowHandlingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.ADD, RTAInfo::reportStats);
                                builder.addPostHook(Phase.ADD, RTAInfo::clear);

                                builder.addPreHook(Phase.ANALYZE, RTAInfo::forceCoreClassesReachable);
                                builder.addElementHandler(Phase.ANALYZE, new ElementBodyCopier());
                                builder.addElementHandler(Phase.ANALYZE, new ElementVisitorAdapter(new DotGenerator(Phase.ANALYZE, graphGenConfig)));
                                builder.addElementHandler(Phase.ANALYZE, new ElementVisitorAdapter(new ConnectionGraphDotGenerator()));
                                if (optGotos) {
                                    builder.addCopyFactory(Phase.ANALYZE, GotoRemovingVisitor::new);
                                }
                                if (optPhis) {
                                    builder.addCopyFactory(Phase.ANALYZE, PhiOptimizerVisitor::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.CORRECT, NumericalConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                if (optInlining) {
                                    builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, InliningBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, EscapeAnalysisIntraMethodBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, LocalVariableFindingBasicBlockBuilder::new);
                                builder.addPostHook(Phase.ANALYZE, RTAInfo::reportStats);
                                builder.addPostHook(Phase.ANALYZE, new ClassInitializerRegister());
                                builder.addPostHook(Phase.ANALYZE, new DispatchTableBuilder());
                                builder.addPostHook(Phase.ANALYZE, new SupersDisplayBuilder());
                                builder.addPostHook(Phase.ANALYZE, new ClassObjectSerializer());
                                builder.addPostHook(Phase.ANALYZE, EscapeAnalysisState::interMethodAnalysis);

                                builder.addCopyFactory(Phase.LOWER, EscapeAnalysisOptimizeVisitor::new);
                                builder.addElementHandler(Phase.LOWER, new FunctionLoweringElementHandler());
                                builder.addElementHandler(Phase.LOWER, new ElementVisitorAdapter(new DotGenerator(Phase.LOWER, graphGenConfig)));
                                if (optGotos) {
                                    builder.addCopyFactory(Phase.LOWER, GotoRemovingVisitor::new);
                                }
                                if (optPhis) {
                                    builder.addCopyFactory(Phase.LOWER, PhiOptimizerVisitor::new);
                                }
                                builder.addCopyFactory(Phase.LOWER, ObjectLiteralSerializingVisitor::new);

                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ThrowLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                if (nogc) {
                                    builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, NoGcBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForLowerPhase);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InvocationLoweringBasicBlockBuilder::new);
                                // BooleanAccessBasicBlockBuilder must come before object and static field access lowering
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, BooleanAccessBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LocalVariableLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, StaticFieldLoweringBasicBlockBuilder::new);
                                // InstanceOfCheckCastBB must come before ObjectAccessLoweringBuilder or typeIdOf won't be lowered correctly
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InstanceOfCheckCastBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LowerClassInitCheckBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectAccessLoweringBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectMonitorBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LLVMCompatibleBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                if (optMemoryTracking) {
                                    builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                                }
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);

                                builder.addPreHook(Phase.GENERATE, new SupersDisplayEmitter());
                                builder.addPreHook(Phase.GENERATE, new DispatchTableEmitter());
                                builder.addPreHook(Phase.GENERATE, new LLVMGenerator(isPie ? 2 : 0, isPie ? 2 : 0));

                                builder.addPostHook(Phase.GENERATE, new DotGenerator(Phase.GENERATE, graphGenConfig));
                                builder.addPostHook(Phase.GENERATE, new LLVMCompileStage(isPie));
                                builder.addPostHook(Phase.GENERATE, new MethodDataEmitter());
                                builder.addPostHook(Phase.GENERATE, new StringPoolEmitter());
                                builder.addPostHook(Phase.GENERATE, new LLVMDefaultModuleCompileStage(isPie));
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
        return initialContext;
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        CommandLineProcessor optionsProcessor = new CommandLineProcessor();
        CmdResult result = optionsProcessor.process(args);
        if (result != CmdResult.CMD_RESULT_OK) {
            return;
        }
        Builder mainBuilder = builder();
        mainBuilder.setBootModulePaths(optionsProcessor.bootPaths)
            .setOutputPath(optionsProcessor.outputPath)
            .setMainClass(optionsProcessor.mainClass)
            .setDiagnosticsHandler(diagnostics -> {
                for (Diagnostic diagnostic : diagnostics) {
                    try {
                        diagnostic.appendTo(System.err);
                    } catch (IOException e) {
                        // just give up
                        break;
                    }
                }
            })
            .setGc(optionsProcessor.gc.toString())
            .setIsPie(optionsProcessor.isPie)
            .setOptMemoryTracking(optionsProcessor.optArgs.optMemoryTracking)
            .setOptInlining(optionsProcessor.optArgs.optInlining)
            .setOptGotos(optionsProcessor.optArgs.optGotos)
            .setOptPhis(optionsProcessor.optArgs.optPhis)
            .setGraphGenConfig(optionsProcessor.graphGenConfig);
        Platform platform = optionsProcessor.platform;
        if (platform != null) {
            mainBuilder.setPlatform(platform);
        }

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

    private enum CmdResult {
        CMD_RESULT_HELP,
        CMD_RESULT_OK,
        CMD_RESULT_ERROR,
        ;
    }

    @CommandLine.Command(version = "1.0", mixinStandardHelpOptions = true)
    private static final class CommandLineProcessor {
        private enum GCType {
            NONE("none"),
            ;
            private final String gcType;

            GCType(String type) {
                this.gcType = type;
            }
            public String toString() {
                return gcType;
            }
        }

        @CommandLine.Option(names = "--boot-module-path", required = true, split=":")
        private String[] bootPaths;
        @CommandLine.Option(names = "--output-path", description = "Specify directory where the executable is placed")
        private Path outputPath;
        @CommandLine.Option(names = "--debug")
        private boolean debug;
        @CommandLine.Option(names = "--debug-vtables")
        private boolean debugVTables;
        @CommandLine.Option(names = "--dispatch-stats")
        private boolean dispatchStats;
        @CommandLine.Option(names = "--debug-rta")
        private boolean debugRTA;
        @CommandLine.Option(names = "--debug-supers")
        private boolean debugSupers;
        @CommandLine.Option(names = "--debug-devirt")
        private boolean debugDevirt;
        @CommandLine.Option(names = "--gc", defaultValue = "none", description = "Type of GC to use. Valid values: ${COMPLETION-CANDIDATES}")
        private GCType gc;
        @CommandLine.Option(names = "--method-data-stats")
        private boolean methodDataStats;
        @CommandLine.Option(names = "--pie", negatable = true, defaultValue = "false", description = "[Disable|Enable] generation of position independent executable")
        private boolean isPie;
        @CommandLine.Option(names = "--platform", converter = PlatformConverter.class)
        private Platform platform;
        @CommandLine.Option(names = "--string-pool-stats")
        private boolean stringPoolStats;

        @CommandLine.Parameters(index="0", arity="1", description = "Application main class")
        private String mainClass;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1", heading = "Options for controlling generation of graphs for methods%n")
        private GraphGenArgs graphGenArgs;

        @CommandLine.ArgGroup(exclusive = false, heading = "Options for controlling optimizations%n")
        private OptArgs optArgs = new OptArgs();

        private GraphGenConfig graphGenConfig = new GraphGenConfig();

        private static class GraphGenArgs {
            @CommandLine.Option(names = { "-g", "--gen-graph"}, required = true, description = "Enable generation of graphs")
            boolean genGraph;
            @CommandLine.ArgGroup(exclusive=false, multiplicity = "0..*")
            List<GraphGenMethodsPhases> methodsAndPhases;
        }

        private static class GraphGenMethodsPhases {
            @CommandLine.Option(names = { "-m", "--methods"}, required = false, split = ",", defaultValue = GraphGenConfig.ALL_METHODS,
                                description = "List of methods separated by comma. Default: ${DEFAULT-VALUE}")
            List<String> methods;
            @CommandLine.Option(names = { "-p", "--phases" }, required = false, split = ",", defaultValue = GraphGenConfig.ALL_PHASES,
                                description = "List of phases separated by comma. Default: ${DEFAULT-VALUE}")
            List<String> phases;
        }

        static class OptArgs {
            @CommandLine.Option(names = "--opt-memory-tracking", negatable = true, defaultValue = "false", description = "Enable/disable redundant store/load tracking and elimination")
            boolean optMemoryTracking;
            @CommandLine.Option(names = "--opt-inlining", negatable = true, defaultValue = "false", description = "Enable/disable inliner")
            boolean optInlining;
            @CommandLine.Option(names = "--opt-phis", negatable = true, defaultValue = "true", description = "Enable/disable `phi` elimination")
            boolean optPhis;
            @CommandLine.Option(names = "--opt-gotos", negatable = true, defaultValue = "true", description = "Enable/disable `goto` elimination")
            boolean optGotos;
        }

        public CmdResult process(String[] args) {
            try {
                ParseResult parseResult = new CommandLine(this).parseArgs(args);
                if (CommandLine.printHelpIfRequested(parseResult)) {
                    return CmdResult.CMD_RESULT_HELP;
                }
            } catch (ParameterException ex) { // command line arguments could not be parsed
                System.err.println(ex.getMessage());
                ex.getCommandLine().usage(System.err);
                return CmdResult.CMD_RESULT_ERROR;
            }

            if (debug) {
                Logger.getLogger("").setLevel(Level.DEBUG);
            }
            if (debugVTables) {
                Logger.getLogger("org.qbicc.plugin.dispatch.tables").setLevel(Level.DEBUG);
            }
            if (dispatchStats) {
                Logger.getLogger("org.qbicc.plugin.dispatch.stats").setLevel(Level.DEBUG);
            }
            if (debugRTA) {
                Logger.getLogger("org.qbicc.plugin.reachability.rta").setLevel(Level.DEBUG);
            }
            if (debugSupers) {
                Logger.getLogger("org.qbicc.plugin.instanceofcheckcast.supers").setLevel(Level.DEBUG);
            }
            if (debugDevirt) {
                Logger.getLogger("org.qbicc.plugin.dispatch.devirt").setLevel(Level.DEBUG);
            }
            if (methodDataStats) {
                Logger.getLogger("org.qbicc.plugin.methodinfo.stats").setLevel(Level.DEBUG);
            }
            if (stringPoolStats) {
                Logger.getLogger("org.qbicc.plugin.stringpool.stats").setLevel(Level.DEBUG);
            }
            if (outputPath == null) {
                outputPath = Path.of(System.getProperty("java.io.tmpdir"), "qbicc-output-" + Integer.toHexString(ThreadLocalRandom.current().nextInt()));
            }

            if (graphGenArgs != null && graphGenArgs.genGraph) {
                if (graphGenArgs.methodsAndPhases == null) {
                    graphGenConfig.addMethodAndPhase(GraphGenConfig.ALL_METHODS, GraphGenConfig.ALL_PHASES);
                } else {
                    for (GraphGenMethodsPhases option : graphGenArgs.methodsAndPhases) {
                        for (String method : option.methods) {
                            for (String phase : option.phases) {
                                graphGenConfig.addMethodAndPhase(method, phase);
                            }
                        }
                    }
                }
            }
            return CmdResult.CMD_RESULT_OK;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Path> bootModulePath = new ArrayList<>();
        private Path outputPath;
        private Consumer<Iterable<Diagnostic>> diagnosticsHandler = diagnostics -> {};
        private Platform platform = Platform.HOST_PLATFORM;
        private String mainClass;
        private String gc = "none";
        // TODO Detect whether the system uses PIEs by default and match that if possible
        private boolean isPie = false;
        private boolean optMemoryTracking = false;
        private boolean optInlining = false;
        private boolean optPhis = true;
        private boolean optGotos = true;
        private GraphGenConfig graphGenConfig;

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

        public Builder setBootModulePaths(String[] paths) {
            Assert.checkNotNullParam("paths", paths);
            for (String pathStr : paths) {
                if (! pathStr.isEmpty()) {
                    addBootModulePath(Path.of(pathStr));
                }
            }
            return this;
        }

        public Builder setOutputPath(Path path) {
            Assert.checkNotNullParam("path", path);
            this.outputPath = path;
            return this;
        }

        public Builder setPlatform(Platform platform) {
            Assert.checkNotNullParam("platform", platform);
            this.platform = platform;
            return this;
        }

        public Builder setDiagnosticsHandler(Consumer<Iterable<Diagnostic>> handler) {
            Assert.checkNotNullParam("handler", handler);
            diagnosticsHandler = handler;
            return this;
        }

        public Builder setMainClass(String mainClass) {
            Assert.checkNotNullParam("mainClass", mainClass);
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

        public Builder setGraphGenConfig(GraphGenConfig graphGenConfig) {
            Assert.checkNotNullParam("graphGenConfig", graphGenConfig);
            this.graphGenConfig = graphGenConfig;
            return this;
        }

        public Builder setOptMemoryTracking(boolean optMemoryTracking) {
            this.optMemoryTracking = optMemoryTracking;
            return this;
        }

        public Builder setOptInlining(boolean optInlining) {
            this.optInlining = optInlining;
            return this;
        }

        public Builder setOptPhis(boolean optPhis) {
            this.optPhis = optPhis;
            return this;
        }

        public Builder setOptGotos(boolean optGotos) {
            this.optGotos = optGotos;
            return this;
        }

        public Main build() {
            return new Main(this);
        }
    }
}
