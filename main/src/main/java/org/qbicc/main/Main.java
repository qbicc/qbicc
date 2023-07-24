package org.qbicc.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.jar.JarInputStream;

import io.smallrye.beanbag.maven.MavenFactory;
import io.smallrye.common.constraint.Assert;
import io.smallrye.common.version.VersionIterator;
import io.smallrye.common.version.VersionScheme;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsProblem;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.logmanager.Level;
import org.jboss.logmanager.LogManager;
import org.jboss.logmanager.Logger;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.DiagnosticContext;
import org.qbicc.context.Location;
import org.qbicc.driver.BaseDiagnosticContext;
import org.qbicc.driver.BuilderStage;
import org.qbicc.driver.ClassPathElement;
import org.qbicc.driver.ClassPathItem;
import org.qbicc.driver.Driver;
import org.qbicc.driver.ElementBodyCopier;
import org.qbicc.driver.ElementBodyCreator;
import org.qbicc.driver.ElementInitializer;
import org.qbicc.driver.ElementVisitorAdapter;
import org.qbicc.driver.GraphGenConfig;
import org.qbicc.driver.Phase;
import org.qbicc.driver.plugin.DriverPlugin;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmThread;
import org.qbicc.interpreter.impl.VmImpl;
import org.qbicc.machine.arch.Platform;
import org.qbicc.machine.object.ObjectFileProvider;
import org.qbicc.machine.tool.CToolChain;
import org.qbicc.machine.vfs.AbsoluteVirtualPath;
import org.qbicc.machine.vfs.VFSUtils;
import org.qbicc.machine.vfs.VirtualFileSystem;
import org.qbicc.plugin.apploader.AppClassLoader;
import org.qbicc.plugin.apploader.InitAppClassLoaderHook;
import org.qbicc.plugin.constants.ConstantBasicBlockBuilder;
import org.qbicc.plugin.conversion.NumericalConversionBasicBlockBuilder;
import org.qbicc.plugin.core.CoreAnnotationTypeBuilder;
import org.qbicc.plugin.coreclasses.BasicHeaderManualInitializer;
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.plugin.coreclasses.CoreClassesBasicBlockBuilder;
import org.qbicc.plugin.correctness.BuildTimeOnlyElementHandler;
import org.qbicc.plugin.correctness.ConstraintMaterializingBasicBlockBuilder;
import org.qbicc.plugin.correctness.DeferenceBasicBlockBuilder;
import org.qbicc.plugin.correctness.RuntimeChecksBasicBlockBuilder;
import org.qbicc.plugin.correctness.StaticChecksBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DevirtualizingBasicBlockBuilder;
import org.qbicc.plugin.dispatch.DispatchTableBuilder;
import org.qbicc.plugin.dispatch.DispatchTableEmitter;
import org.qbicc.plugin.dot.DotGenerator;
import org.qbicc.plugin.gc.common.AbstractGc;
import org.qbicc.plugin.gc.common.GcBasicBlockBuilder;
import org.qbicc.plugin.gc.common.GcCommon;
import org.qbicc.plugin.gc.common.MultiNewArrayExpansionBasicBlockBuilder;
import org.qbicc.plugin.gc.common.safepoint.SafePointPlacementBasicBlockBuilder;
import org.qbicc.plugin.gc.common.safepoint.SafePoints;
import org.qbicc.plugin.initializationcontrol.InitAtRuntimeTypeBuilder;
import org.qbicc.plugin.initializationcontrol.QbiccFeature;
import org.qbicc.plugin.initializationcontrol.QbiccFeatureProcessor;
import org.qbicc.plugin.initializationcontrol.RuntimeResourceManager;
import org.qbicc.plugin.instanceofcheckcast.InstanceOfCheckCastBasicBlockBuilder;
import org.qbicc.plugin.instanceofcheckcast.InvalidCastsCleanupBasicBlockBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayBuilder;
import org.qbicc.plugin.instanceofcheckcast.SupersDisplayEmitter;
import org.qbicc.plugin.intrinsics.IntrinsicBasicBlockBuilder;
import org.qbicc.plugin.intrinsics.core.CoreIntrinsics;
import org.qbicc.plugin.layout.ObjectAccessLoweringBuilder;
import org.qbicc.plugin.linker.LinkStage;
import org.qbicc.plugin.llvm.LLVMCompatibleBasicBlockBuilder;
import org.qbicc.plugin.llvm.LLVMConfiguration;
import org.qbicc.plugin.llvm.LLVMDefaultModuleCompileStage;
import org.qbicc.plugin.llvm.LLVMGenerator;
import org.qbicc.plugin.llvm.LLVMIntrinsics;
import org.qbicc.plugin.llvm.LLVMStackMapCollector;
import org.qbicc.plugin.llvm.LLVMStripStackMapStage;
import org.qbicc.plugin.llvm.ReferenceStrategy;
import org.qbicc.plugin.lowering.AbortingThrowLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.BooleanAccessCopier;
import org.qbicc.plugin.lowering.FunctionLoweringElementHandler;
import org.qbicc.plugin.lowering.InitCheckLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.InvocationLoweringBasicBlockBuilder;
import org.qbicc.plugin.lowering.MemberPointerCopier;
import org.qbicc.plugin.lowering.VMHelpersSetupHook;
import org.qbicc.plugin.main_method.AddMainClassHook;
import org.qbicc.plugin.main_method.MainMethod;
import org.qbicc.plugin.methodinfo.CallSiteTable;
import org.qbicc.plugin.native_.ConstTypeResolver;
import org.qbicc.plugin.native_.ConstantDefiningBasicBlockBuilder;
import org.qbicc.plugin.native_.ExternExportTypeBuilder;
import org.qbicc.plugin.native_.FunctionTypeResolver;
import org.qbicc.plugin.native_.InternalNativeTypeResolver;
import org.qbicc.plugin.native_.NativeBasicBlockBuilder;
import org.qbicc.plugin.native_.NativeBindingMethodConfigurator;
import org.qbicc.plugin.native_.NativeTypeBuilder;
import org.qbicc.plugin.native_.NativeTypeResolver;
import org.qbicc.plugin.native_.NativeXtorLoweringHook;
import org.qbicc.plugin.native_.PointerBasicBlockBuilder;
import org.qbicc.plugin.native_.PointerTypeResolver;
import org.qbicc.plugin.native_.StructMemberAccessBasicBlockBuilder;
import org.qbicc.plugin.objectmonitor.ObjectMonitorBasicBlockBuilder;
import org.qbicc.plugin.opt.BlockParameterOptimizingVisitor;
import org.qbicc.plugin.opt.FinalFieldLoadOptimizer;
import org.qbicc.plugin.opt.GotoRemovingVisitor;
import org.qbicc.plugin.opt.InliningBasicBlockBuilder;
import org.qbicc.plugin.opt.LocalMemoryTrackingBasicBlockBuilder;
import org.qbicc.plugin.opt.LocalOptBasicBlockBuilder;
import org.qbicc.plugin.opt.ea.EscapeAnalysisDotGenerator;
import org.qbicc.plugin.opt.ea.EscapeAnalysisDotVisitor;
import org.qbicc.plugin.opt.ea.EscapeAnalysisInterMethodAnalysis;
import org.qbicc.plugin.opt.ea.EscapeAnalysisIntraMethodAnalysis;
import org.qbicc.plugin.opt.ea.EscapeAnalysisOptimizeVisitor;
import org.qbicc.plugin.patcher.AccessorBasicBlockBuilder;
import org.qbicc.plugin.patcher.AccessorTypeBuilder;
import org.qbicc.plugin.patcher.Patcher;
import org.qbicc.plugin.patcher.PatcherResolverBasicBlockBuilder;
import org.qbicc.plugin.patcher.PatcherTypeResolver;
import org.qbicc.plugin.reachability.ReachabilityAnnotationTypeBuilder;
import org.qbicc.plugin.reachability.ReachabilityBlockBuilder;
import org.qbicc.plugin.reachability.ReachabilityFactsSetup;
import org.qbicc.plugin.reachability.ReachabilityInfo;
import org.qbicc.plugin.reachability.ReachabilityRoots;
import org.qbicc.plugin.reachability.ServiceLoaderAnalyzer;
import org.qbicc.plugin.reflection.Reflection;
import org.qbicc.plugin.reflection.ReflectionFactsSetup;
import org.qbicc.plugin.reflection.ReflectionIntrinsics;
import org.qbicc.plugin.reflection.ReflectiveElementRegistry;
import org.qbicc.plugin.reflection.ReflectiveElementTypeBuilder;
import org.qbicc.plugin.reflection.VarHandleResolvingBasicBlockBuilder;
import org.qbicc.plugin.serialization.BuildtimeHeap;
import org.qbicc.plugin.serialization.ClassObjectSerializer;
import org.qbicc.plugin.serialization.InitialHeapLiteralSerializingVisitor;
import org.qbicc.plugin.serialization.MethodDataStringsSerializer;
import org.qbicc.plugin.serialization.StringInternTableEmitter;
import org.qbicc.plugin.source.SourceEmittingElementHandler;
import org.qbicc.plugin.threadlocal.ThreadLocalBasicBlockBuilder;
import org.qbicc.plugin.threadlocal.ThreadLocalTypeBuilder;
import org.qbicc.plugin.trycatch.ExceptionOnThreadStrategy;
import org.qbicc.plugin.trycatch.SynchronizedMethodBasicBlockBuilder;
import org.qbicc.plugin.unwind.UnwindExceptionStrategy;
import org.qbicc.plugin.unwind.UnwindThrowBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassInitializingBasicBlockBuilder;
import org.qbicc.plugin.verification.ClassLoadingBasicBlockBuilder;
import org.qbicc.plugin.verification.LowerVerificationBasicBlockBuilder;
import org.qbicc.plugin.verification.MemberResolvingBasicBlockBuilder;
import org.qbicc.plugin.vfs.VFS;
import org.qbicc.plugin.vio.VIO;
import org.qbicc.tool.llvm.LlvmToolChain;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.classfile.BciRangeExceptionHandlerBasicBlockBuilder;
import org.qbicc.type.definition.classfile.IndyResolvingBasicBlockBuilder;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

/**
 * The main entry point, which can be constructed using a builder or directly invoked.
 */
public class Main implements Callable<DiagnosticContext> {
    private final List<ClassPathEntry> bootPaths;
    private final List<ClassPathEntry> appPaths;
    private final Runtime.Version classLibVersion;
    private final Path outputPath;
    private final String outputName;
    private final Path sourceOutputPath;
    private final Consumer<Iterable<Diagnostic>> diagnosticsHandler;
    private final String mainClass;
    private final List<String> buildTimeInitRootClasses;
    private final List<String> propertyDefines; // format: name=value
    private final String gc;
    private final boolean isPie;
    private final GraphGenConfig graphGenConfig;
    private final boolean outputDot;
    private final boolean compileOutput;
    private final boolean optMemoryTracking;
    private final boolean optPhis;
    private final boolean optGotos;
    private final boolean optInlining;
    private final boolean optEscapeAnalysis;
    private final Platform platform;
    private final boolean smallTypeIds;
    private final List<Path> librarySearchPaths;
    private final List<URL> qbiccYamlFeatures;
    private final List<QbiccFeature> qbiccFeatures;
    private final ClassPathResolver classPathResolver;
    private final Backend backend;
    private final LLVMConfiguration.Builder llvmConfigurationBuilder;
    private final int optLevel;

    Main(Builder builder) {
        outputPath = builder.outputPath;
        outputName = builder.outputName;
        sourceOutputPath = builder.sourceOutputPath;
        diagnosticsHandler = builder.diagnosticsHandler;
        // todo: this becomes optional
        mainClass = Assert.checkNotEmptyParam("builder.mainClass", builder.mainClass);
        buildTimeInitRootClasses = builder.buildTimeInitRootClasses;
        propertyDefines = builder.propertyDefines;
        gc = builder.gc;
        isPie = builder.isPie;
        graphGenConfig = builder.graphGenConfig;
        outputDot = builder.outputDot;
        compileOutput = builder.compileOutput;
        optMemoryTracking = builder.optMemoryTracking;
        optInlining = builder.optInlining;
        optPhis = builder.optPhis;
        optGotos = builder.optGotos;
        optEscapeAnalysis = false && builder.optEscapeAnalysis;
        optLevel = builder.optLevel;
        platform = builder.platform;
        smallTypeIds = builder.smallTypeIds;
        backend = builder.backend;
        ArrayList<ClassPathEntry> bootPaths = new ArrayList<>(builder.bootPathsPrepend.size() + 6 + builder.bootPathsAppend.size());
        bootPaths.addAll(builder.bootPathsPrepend);
        // add core things
        bootPaths.add(getCoreComponent("qbicc-runtime-api"));
        bootPaths.add(getCoreComponent("qbicc-runtime-llvm"));
        bootPaths.add(getCoreComponent("qbicc-runtime-main"));
        bootPaths.add(getCoreComponent("qbicc-runtime-unwind"));
        bootPaths.add(ClassPathEntry.ofClassLibraries(builder.classLibVersion));
        bootPaths.addAll(builder.bootPathsAppend);
        this.bootPaths = bootPaths;
        appPaths = List.copyOf(builder.appPaths);
        librarySearchPaths = builder.librarySearchPaths;
        qbiccYamlFeatures = builder.qbiccYamlFeatures;
        qbiccFeatures = builder.qbiccFeatures;
        classPathResolver = builder.classPathResolver == null ? this::resolveClassPath : builder.classPathResolver;
        llvmConfigurationBuilder = builder.llvmConfigurationBuilder;
        classLibVersion = Runtime.Version.parse(builder.classLibVersion.split("\\.")[0]);
    }

    public DiagnosticContext call() {
        BaseDiagnosticContext ctxt = new BaseDiagnosticContext();
        // 16 MB is the default stack size
        long stackSize = 0x1000000L;
        Thread mainThread = new Thread(Thread.currentThread().getThreadGroup(), () -> {
            try {
                call0(ctxt);
            } catch (Throwable t) {
                t.printStackTrace(System.err);
                ctxt.error(t, "Compilation failed due to an exception");
            }
        }, "Main compiler thread", stackSize, false);
        mainThread.start();
        boolean intr = false;
        try {
            for (;;) try {
                mainThread.join();
                break;
            } catch (InterruptedException ie) {
                mainThread.interrupt();
                intr = true;
            }
            diagnosticsHandler.accept(ctxt.getDiagnostics());
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
        return ctxt;
    }

    void call0(BaseDiagnosticContext initialContext) {
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        builder.setOptLevel(optLevel);
        boolean semiGc = gc.equals("semi");
        boolean llvm = backend.equals(Backend.llvm);
        int errors = initialContext.errors();
        if (errors == 0) {
            builder.setOutputDirectory(outputPath);
            List<ClassPathItem> bootItems = new ArrayList<>();
            List<ClassPathItem> appItems = new ArrayList<>();
            // process the class paths
            try {
                classPathResolver.resolveClassPath(initialContext, bootItems::add, bootPaths, classLibVersion);
            } catch (IOException e) {
                // todo: close class path items?
                return;
            }
            try {
                classPathResolver.resolveClassPath(initialContext, appItems::add, appPaths, classLibVersion);
            } catch (IOException e) {
                return;
            }
            bootItems.forEach(builder::addBootClassPathItem);
            appItems.forEach(builder::addAppClassPathItem);
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
                    try {
                        PlatformTypeSystemLoader platformTypeSystemLoader = new PlatformTypeSystemLoader(
                            platform, toolChain, objectFileProvider, initialContext,
                            PlatformTypeSystemLoader.ReferenceType.POINTER,
                            smallTypeIds);
                        TypeSystem typeSystem = platformTypeSystemLoader.load();

                        builder.setTypeSystem(typeSystem);
                        // add additional manual initializers by chaining `.andThen(...)`
                        builder.setVmFactory(cc -> {
                            AbstractGc.reserveMovedBit(cc);
                            CoreClasses.reserveStackAllocatedBit(cc);
                            QbiccFeatureProcessor.process(cc, qbiccYamlFeatures, qbiccFeatures);
                            CoreClasses.init(cc);
                            ExceptionOnThreadStrategy.initialize(cc);
                            UnwindExceptionStrategy.init(cc);
                            return VmImpl.create(cc,
                                new BasicHeaderManualInitializer(cc)
                            ).setPropertyDefines(this.propertyDefines);
                        });
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
                        LlvmToolChain llvmToolChain = null;
                        LLVMConfiguration tempLlVmConfiguration = null;
                        if (errors == 0 && llvm) {
                            Iterator<LlvmToolChain> llvmTools = LlvmToolChain.findAllLlvmToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
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
                                final VersionIterator vi = VersionScheme.BASIC.iterate(llvmToolChain.getVersion());
                                vi.next();
                                tempLlVmConfiguration = llvmConfigurationBuilder.setMajorVersion(vi.getNumberPartAsInt()).build();
                            }
                        }
                        LLVMConfiguration llvmConfiguration = tempLlVmConfiguration;
                        if (errors == 0) {
                            assert mainClass != null; // else errors would be != 0
                            // keep it simple to start with
                            builder.setMainClass(mainClass.replace('.', '/'));

                            builder.addTypeBuilderFactory(ExternExportTypeBuilder::new);
                            builder.addTypeBuilderFactory(NativeTypeBuilder::new);
                            builder.addTypeBuilderFactory(ThreadLocalTypeBuilder::new);
                            builder.addTypeBuilderFactory(CoreAnnotationTypeBuilder::new);
                            builder.addTypeBuilderFactory(ReachabilityAnnotationTypeBuilder::new);
                            builder.addTypeBuilderFactory(ReflectiveElementTypeBuilder::new);
                            builder.addTypeBuilderFactory(Patcher::getTypeBuilder);
                            builder.addTypeBuilderFactory(InitAtRuntimeTypeBuilder::new);
                            builder.addTypeBuilderFactory(AccessorTypeBuilder::new);

                            builder.setClassContextListener(Patcher::initialize);

                            builder.addNativeMethodConfiguratorFactory(NativeBindingMethodConfigurator::new);

                            builder.addResolverFactory(PatcherTypeResolver::create);
                            builder.addResolverFactory(ConstTypeResolver::new);
                            builder.addResolverFactory(FunctionTypeResolver::new);
                            builder.addResolverFactory(PointerTypeResolver::new);
                            builder.addResolverFactory(InternalNativeTypeResolver::new);
                            builder.addResolverFactory(NativeTypeResolver::new);

                            // from this point on, all pre-ADD hook tasks are run within a VM thread
                            builder.addPreHook(Phase.ADD, c -> {
                                final Vm vm = c.getVm();
                                ThreadLocal<VmThread> threadHolder = ThreadLocal.withInitial(() ->
                                    vm.newThread(Thread.currentThread().getName(), vm.getMainThreadGroup(), false, Thread.currentThread().getPriority())
                                );
                                c.setTaskRunner((wrapper, ctxt) ->
                                    vm.doAttached(threadHolder.get(), () -> wrapper.accept(ctxt))
                                );
                            });
                            builder.addPreHook(Phase.ADD, ReflectionFactsSetup::setupAdd);
                            if (llvm) {
                                builder.addPreHook(Phase.ADD, LLVMIntrinsics::register);
                            }
                            builder.addPreHook(Phase.ADD, CoreIntrinsics::register);
                            builder.addPreHook(Phase.ADD, CoreClasses::get);
                            builder.addPreHook(Phase.ADD, ReflectionIntrinsics::register);
                            builder.addPreHook(Phase.ADD, Reflection::get);
                            builder.addPreHook(Phase.ADD, UnwindExceptionStrategy::get);
                            builder.addPreHook(Phase.ADD, GcCommon::registerIntrinsics);
                            builder.addPreHook(Phase.ADD, ctxt -> AbstractGc.install(ctxt, gc));
                            builder.addPreHook(Phase.ADD, compilationContext -> compilationContext.getVm().initialize());
                            builder.addPreHook(Phase.ADD, VIO::get);
                            builder.addPreHook(Phase.ADD, VFS::initialize);
                            builder.addPreHook(Phase.ADD, Main::mountInitialFileSystem);
                            builder.addPreHook(Phase.ADD, new VMHelpersSetupHook());
                            builder.addPreHook(Phase.ADD, new InitAppClassLoaderHook());
                            builder.addPreHook(Phase.ADD, compilationContext -> compilationContext.getVm().initialize2());
                            builder.addPreHook(Phase.ADD, new AddMainClassHook());
                            // common GC setup
                            builder.addPreHook(Phase.ADD, ReachabilityInfo::forceCoreClassesReachable);
                            builder.addPreHook(Phase.ADD, ReflectiveElementRegistry::ensureReflectiveClassesLoaded);
                            builder.addPreHook(Phase.ADD, ReachabilityFactsSetup::setupAdd);
                            builder.addPreHook(Phase.ADD, compilationContext -> {
                                if (!buildTimeInitRootClasses.isEmpty()) {
                                    for (String toInit : buildTimeInitRootClasses) {
                                        compilationContext.submitTask(toInit, className -> {
                                            Vm vm = compilationContext.getVm();
                                            VmThread loadingThread = vm.newThread("build time init", vm.getMainThreadGroup(), false, Thread.currentThread().getPriority());
                                            VmClassLoader appClassLoader = AppClassLoader.get(compilationContext).getAppClassLoader();
                                            vm.doAttached(loadingThread, () -> {
                                                VmClass vmClass = appClassLoader.loadClass(className.replace('.', '/'));
                                                vm.initialize(vmClass);
                                            });
                                        });
                                    }
                                }
                            });
                            builder.addPreHook(Phase.ADD, new ElementReachableAdapter(
                                new ElementBodyCreator()
                                .andThen(new BuildTimeOnlyElementHandler())
                                .andThen(new ElementInitializer())));
                            builder.addPreHook(Phase.ADD, new ElementReachableAdapter(ReachabilityInfo::processReachableElement));
                            if (outputDot) {
                                builder.addPreHook(Phase.ADD, new ElementReachableAdapter(new ElementVisitorAdapter(new DotGenerator(Phase.ADD, graphGenConfig))));
                            }
                            builder.addPreHook(Phase.ADD, new ElementReachableAdapter(CallSiteTable::computeMethodType));
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForAddPhase);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MultiNewArrayExpansionBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, PatcherResolverBasicBlockBuilder::createIfNeeded);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassLoadingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, VarHandleResolvingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MemberResolvingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, AccessorBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, StructMemberAccessBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, PointerBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassInitializingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantDefiningBasicBlockBuilder::createIfNeeded);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, CoreClassesBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, InvalidCastsCleanupBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, BciRangeExceptionHandlerBasicBlockBuilder::createIfNeeded);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IndyResolvingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, SynchronizedMethodBasicBlockBuilder::createIfNeeded);
                            if (optMemoryTracking) {
                                // TODO: breaks addr_of; should only be done in ANALYZE and then only if addr_of wasn't taken (alias)
                                // builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                            }
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, RuntimeChecksBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.OPTIMIZE, LocalOptBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, DeferenceBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, StaticChecksBasicBlockBuilder::new);
                            builder.addPostHook(Phase.ADD, ctxt -> {
                                Vm vm = ctxt.getVm();
                                vm.doAttached(vm.newThread("ReflectionData Generation", vm.getMainThreadGroup(), false, Thread.currentThread().getPriority()), () -> {
                                    Reflection.get(ctxt).transferToReflectionData();
                                });
                            });
                            builder.addPostHook(Phase.ADD, ctxt -> {
                                Vm vm = ctxt.getVm();
                                vm.doAttached(vm.newThread("ServiceProvider Serialization", vm.getMainThreadGroup(), false, Thread.currentThread().getPriority()), () -> {
                                    ServiceLoaderAnalyzer.get(ctxt).serializeProviderConfig();
                                });
                            });
                            builder.addPostHook(Phase.ADD, ctxt -> {
                                Vm vm = ctxt.getVm();
                                vm.doAttached(vm.newThread("Resource Serialization", vm.getMainThreadGroup(), false, Thread.currentThread().getPriority()), () -> {
                                    RuntimeResourceManager.get(ctxt).findAndSerializeResources();
                                });
                            });

                            builder.addPreHook(Phase.ANALYZE, ReachabilityInfo::reportStats);
                            builder.addPreHook(Phase.ANALYZE, ReachabilityInfo::clear);
                            builder.addPreHook(Phase.ANALYZE, ReachabilityFactsSetup::setupAnalyze);
                            builder.addPreHook(Phase.ANALYZE, new VMHelpersSetupHook());
                            builder.addPreHook(Phase.ANALYZE, ReachabilityInfo::forceCoreClassesReachable);
                            builder.addPreHook(Phase.ANALYZE, ReachabilityRoots::processRootsForAnalyze);
                            builder.addPreHook(Phase.ANALYZE, new ElementReachableAdapter(ReachabilityInfo::processReachableElement));
                            builder.addPreHook(Phase.ANALYZE, new ElementReachableAdapter(new ElementBodyCopier()));
                            if (optEscapeAnalysis) {
                                builder.addPreHook(Phase.ANALYZE, new ElementReachableAdapter(new ElementVisitorAdapter(new EscapeAnalysisIntraMethodAnalysis())));
                                if (outputDot) {
                                    builder.addPreHook(Phase.ANALYZE, new ElementReachableAdapter(new ElementVisitorAdapter(
                                        new DotGenerator(Phase.ANALYZE, "analyze-intra", graphGenConfig).addVisitorFactory(EscapeAnalysisDotVisitor::new))
                                    ));
                                }
                            } else if (outputDot) {
                                builder.addPreHook(Phase.ANALYZE, new ElementReachableAdapter(new ElementVisitorAdapter(new DotGenerator(Phase.ANALYZE, graphGenConfig))));
                            }
                            if (optGotos) {
                                builder.addCopyFactory(Phase.ANALYZE, GotoRemovingVisitor::new);
                            }
                            if (optPhis) {
                                builder.addCopyFactory(Phase.ANALYZE, BlockParameterOptimizingVisitor::new);
                            }
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForAnalyzePhase);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, FinalFieldLoadOptimizer::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, ThreadLocalBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                            if (optMemoryTracking) {
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                            }
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, ConstraintMaterializingBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.TRANSFORM, InvalidCastsCleanupBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.CORRECT, NumericalConversionBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, LocalOptBasicBlockBuilder::new);
                            if (optInlining) {
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, InliningBasicBlockBuilder::createIfNeeded);
                            }
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                            builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, StaticChecksBasicBlockBuilder::new);

                            if (optEscapeAnalysis) {
                                builder.addPostHook(Phase.ANALYZE, new EscapeAnalysisInterMethodAnalysis());
                                if (outputDot) {
                                    builder.addPostHook(Phase.ANALYZE, new EscapeAnalysisDotGenerator(graphGenConfig));
                                }
                            }

                            builder.addPreHook(Phase.LOWER, ReachabilityInfo::reportStats);
                            builder.addPreHook(Phase.LOWER, new DispatchTableBuilder());
                            builder.addPreHook(Phase.LOWER, new SupersDisplayBuilder());
                            builder.addPreHook(Phase.LOWER, ReachabilityFactsSetup::setupLower);
                            builder.addPreHook(Phase.LOWER, ReachabilityRoots::processRootsForLower);
                            builder.addPreHook(Phase.LOWER, new ClassObjectSerializer());
                            if (optEscapeAnalysis) {
                                builder.addCopyFactory(Phase.LOWER, EscapeAnalysisOptimizeVisitor::new);
                            }
                            builder.addPreHook(Phase.LOWER, new ElementReachableAdapter(new FunctionLoweringElementHandler()));
                            if (outputDot) {
                                builder.addPreHook(Phase.LOWER, new ElementReachableAdapter(new ElementVisitorAdapter(new DotGenerator(Phase.LOWER, graphGenConfig))));
                            }
                            if (sourceOutputPath != null) {
                                Map<ClassContext, List<ClassPathElement>> sourcePaths = new HashMap<>();
                                builder.addPreHook(Phase.LOWER, ctxt -> createSourcePaths(ctxt, bootItems, appItems, sourcePaths));
                                builder.addPreHook(Phase.LOWER, new ElementReachableAdapter(new SourceEmittingElementHandler(sourceOutputPath, sourcePaths)));
                            }
                            if (optGotos) {
                                builder.addCopyFactory(Phase.LOWER, GotoRemovingVisitor::new);
                            }
                            if (optPhis) {
                                builder.addCopyFactory(Phase.LOWER, BlockParameterOptimizingVisitor::new);
                            }
                            builder.addCopyFactory(Phase.LOWER, BooleanAccessCopier::new);
                            builder.addCopyFactory(Phase.LOWER, InitialHeapLiteralSerializingVisitor::new);
                            builder.addCopyFactory(Phase.LOWER, MemberPointerCopier::new);

                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, SafePointPlacementBasicBlockBuilder::createIfNeeded);
                            if (platform.isWasm()) {
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, AbortingThrowLoweringBasicBlockBuilder::new);
                            } else {
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ExceptionOnThreadStrategy::loweringBuilder);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, UnwindThrowBasicBlockBuilder::new);
                            }
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                            // use the common GC BBB for most algorithms
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, GcBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::createForLowerPhase);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InvocationLoweringBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InstanceOfCheckCastBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InitCheckLoweringBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectAccessLoweringBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectMonitorBasicBlockBuilder::new);
                            if (llvm) {
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, (ctxt, delegate) -> new LLVMCompatibleBasicBlockBuilder(ctxt, delegate, llvmConfiguration));
                            }
                            if (optMemoryTracking) {
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, LocalMemoryTrackingBasicBlockBuilder::new);
                            }
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, SafePoints::createBasicBlockBuilder);
                            // To avoid serializing Strings we won't need, MethodDataStringsSerializer should be the last "real" BBB
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, MethodDataStringsSerializer::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.OPTIMIZE, LocalOptBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);
                            builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, StaticChecksBasicBlockBuilder::new);
                            builder.addPostHook(Phase.LOWER, NativeXtorLoweringHook::process);
                            builder.addPostHook(Phase.LOWER, BuildtimeHeap::reportStats);

                            builder.addPreHook(Phase.GENERATE, ReachabilityFactsSetup::setupGenerate);
                            builder.addPreHook(Phase.GENERATE, new StringInternTableEmitter());
                            builder.addPreHook(Phase.GENERATE, new SupersDisplayEmitter());
                            builder.addPreHook(Phase.GENERATE, new DispatchTableEmitter());
                            builder.addPreHook(Phase.GENERATE, BuildtimeHeap::emitEndMarkers);

                            if (llvm) {
                                builder.addPreHook(Phase.GENERATE, new LLVMGenerator(llvmConfiguration));
                            }

                            if (outputDot) {
                                builder.addPostHook(Phase.GENERATE, new DotGenerator(Phase.GENERATE, graphGenConfig));
                            }
                            if (llvm) {
                                if (llvmConfiguration.isStatepointEnabled()) {
                                    builder.addPostHook(Phase.GENERATE, LLVMStackMapCollector::execute);
                                    builder.addPostHook(Phase.GENERATE, new LLVMStripStackMapStage());
                                }
                            }
                            if (! platform.isWasm()) {
                                // todo: have a flag for callSiteTable vs shadow stack
                                builder.addPostHook(Phase.GENERATE, CallSiteTable::writeCallSiteTable);
                            }
                            if (llvm) {
                                // todo: have a flag for callSiteTable vs shadow stack
                                builder.addPostHook(Phase.GENERATE, new LLVMDefaultModuleCompileStage(llvmConfiguration));
                            }
                            if (compileOutput) {
                                builder.addPostHook(Phase.GENERATE, new LinkStage(outputName, isPie, librarySearchPaths));
                            }
                            CompilationContext ctxt;
                            try (Driver driver = builder.build()) {
                                ctxt = driver.getCompilationContext();
                                MainMethod.get(ctxt).setMainClass(mainClass);
                                driver.execute();
                            }
                        }
                    } catch (IOException e) {
                        initialContext.error(e, "Failed to probe system types from tool chain");
                    }
                }
            }
        }
        return;
    }

    private void createSourcePaths(final CompilationContext ctxt, List<ClassPathItem> bootItems, List<ClassPathItem> appItems, final Map<ClassContext, List<ClassPathElement>> sourcePaths) {
        // boot context
        ClassContext bootCtxt = ctxt.getBootstrapClassContext();
        // platform context
        VmClassLoader platCl = ctxt.getVm().getPlatformClassLoader();
        ClassContext platCtxt = platCl == null ? null : platCl.getClassContext();
        // app context
        VmClassLoader appCl = ctxt.getVm().getAppClassLoader();
        ClassContext appCtxt = appCl == null ? null : appCl.getClassContext();
        // the final map...
        List<ClassPathElement> sourceElements = getSourceEntries(bootItems);
        // todo: add class loader distinguishing name to both source file name and output directory.
        // for now, use the same list for both the boot and platform contexts.
        sourcePaths.put(bootCtxt, sourceElements);
        if (platCtxt != null) {
            sourcePaths.put(platCtxt, sourceElements);
        }
        // use the app one for the app context.
        if (appCtxt != null) {
            sourcePaths.put(appCtxt, getSourceEntries(appItems));
        }
    }

    private List<ClassPathElement> getSourceEntries(final List<ClassPathItem> items) {
        List<ClassPathElement> output = new ArrayList<>(64);
        for (ClassPathItem item : items) {
            output.addAll(item.sourceRoots());
        }
        return output;
    }

    private ClassPathEntry getCoreComponent(final String artifactId) {
        return ClassPathEntry.of(new DefaultArtifact("org.qbicc", artifactId, "jar", Version.QBICC_VERSION));
    }

    private void resolveClassPath(DiagnosticContext ctxt, Consumer<ClassPathItem> classPathItemConsumer, final List<ClassPathEntry> paths, Runtime.Version version) throws IOException {
        MavenFactory mavenFactory = MavenFactory.create();
        final RepositorySystem system = mavenFactory.getRepositorySystem();
        File globalSettings = MavenFactory.getGlobalSettingsLocation();
        File userSettings = MavenFactory.getUserSettingsLocation();
        Settings settings;
        try {
            settings = mavenFactory.createSettingsFromContainer(globalSettings, userSettings, problem -> {
                Location loc = Location.builder().setSourceFilePath(problem.getSource()).setLineNumber(problem.getLineNumber()).build();
                SettingsProblem.Severity severity = problem.getSeverity();
                Diagnostic.Level level = switch (severity) {
                    case FATAL, ERROR -> Diagnostic.Level.ERROR;
                    case WARNING -> Diagnostic.Level.WARNING;
                };
                ctxt.msg(null, loc, level, "Maven settings problem: %s", problem.getMessage());
            });
        } catch (SettingsBuildingException e) {
            throw new IOException(e);
        }
        final List<RemoteRepository> remoteRepositoryList = MavenFactory.createRemoteRepositoryList(settings);
        RepositorySystemSession session = mavenFactory.createSession(settings);
        final DefaultArtifactRequestor requestor = new DefaultArtifactRequestor();
        final List<ClassPathItem> result = requestor.requestArtifactsFromRepositories(system, session, remoteRepositoryList, paths, ctxt, version);
        result.forEach(classPathItemConsumer);
    }

    static List<Path> splitPathString(String str) {
        if (str == null || str.isEmpty()) {
            return List.of();
        }
        char psc = File.pathSeparatorChar;
        int start = 0;
        int idx = str.indexOf(psc);
        ArrayList<Path> list = new ArrayList<>();
        for (;;) {
            String subStr;
            if (idx == -1) {
                subStr = str.substring(start);
            } else {
                subStr = str.substring(start, idx);
            }
            if (! subStr.isEmpty()) {
                list.add(Path.of(subStr));
            }
            if (idx == -1) {
                return list;
            } else {
                start = idx + 1;
            }
            idx = str.indexOf(psc, start);
        }
    }

    private static void mountInitialFileSystem(CompilationContext ctxt) {
        // install all boot classpath items into the VFS
        VFS vfs = VFS.get(ctxt);
        VirtualFileSystem fileSystem = vfs.getFileSystem();
        Driver driver = Driver.get(ctxt);
        AbsoluteVirtualPath javaHome = vfs.getQbiccPath().resolve("java.home");
        try {
            //noinspection OctalInteger
            fileSystem.mkdirs(javaHome, 0755);
        } catch (IOException e) {
            ctxt.error(e, "Failed to create %s", javaHome);
        }
        AbsoluteVirtualPath modulesPath = javaHome.resolve("modules");
        Collection<String> bootModuleNames = driver.getBootModuleNames();
        for (String bootModuleName : bootModuleNames) {
            ClassPathItem bootItem = driver.getBootModuleClassPathItem(bootModuleName);
            try {
                bootItem.mount(fileSystem, modulesPath.resolve(bootModuleName));
            } catch (IOException e) {
                ctxt.error(e, "Failed to mount %s", bootItem);
            }
        }
        // now look for all META-INF/java.home files and link them into the main system
        for (String bootModuleName : bootModuleNames) {
            AbsoluteVirtualPath moduleJavaHome = modulesPath.resolve(bootModuleName).resolve("META-INF").resolve("java.home");
            try {
                int attrs = fileSystem.getBooleanAttributes(moduleJavaHome, false);
                if ((attrs & VFSUtils.BA_EXISTS) != 0) {
                    // do it
                    linkIn(fileSystem, javaHome, moduleJavaHome);
                }
            } catch (IOException e) {
                ctxt.error(e, "Failed to remount %s", moduleJavaHome);
            }
        }
    }

    private static void linkIn(final VirtualFileSystem fileSystem, final AbsoluteVirtualPath toDir, final AbsoluteVirtualPath fromDir) throws IOException {
        Collection<String> names = fileSystem.getDirectoryEntries(fromDir, false);
        for (String name : names) {
            AbsoluteVirtualPath fromPath = fromDir.resolve(name);
            AbsoluteVirtualPath toPath = toDir.resolve(name);
            //noinspection OctalInteger
            fileSystem.mkdirs(toPath.getParent(), 0755);
            if ((fileSystem.getBooleanAttributes(fromPath, false) & VFSUtils.BA_DIRECTORY) != 0) {
                linkIn(fileSystem, toPath, fromPath);
            } else {
                fileSystem.link(toPath, fromPath, false);
            }
        }
    }

    public static void main(String[] args) {
        System.setProperty("java.util.logging.manager", LogManager.class.getName());
        CommandLineProcessor optionsProcessor = new CommandLineProcessor();
        CmdResult result = optionsProcessor.process(args);
        if (result != CmdResult.CMD_RESULT_OK) {
            return;
        }
        if (optionsProcessor.mainClass.isEmpty() && optionsProcessor.inputJar == null) {
            System.err.println("Must either provide a <mainClass> or use --jar <executableJar>");
            return;
        }
        Platform platform = optionsProcessor.platform;
        if (platform == null) {
            platform = Platform.HOST_PLATFORM;
        }
        Builder mainBuilder = builder();
        mainBuilder.setClassLibVersion(optionsProcessor.rtVersion)
            .appendBootPaths(optionsProcessor.appendedBootPathEntries)
            .prependBootPaths(optionsProcessor.prependedBootPathEntries)
            .addAppPaths(optionsProcessor.appPathEntries)
            .processJarArgument(optionsProcessor.inputJar)
            .addQbiccYamlFeatures(optionsProcessor.qbiccFeatures.stream().toList())
            .setOutputPath(optionsProcessor.outputPath)
            .setOutputName(optionsProcessor.outputName)
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
            .setCompileOutput(optionsProcessor.compileOutput)
            .setOptMemoryTracking(optionsProcessor.optArgs.optMemoryTracking)
            .setOptInlining(optionsProcessor.optArgs.optInlining)
            .setOptGotos(optionsProcessor.optArgs.optGotos)
            .setOptPhis(optionsProcessor.optArgs.optPhis)
            .setOptEscapeAnalysis(optionsProcessor.optArgs.optEscapeAnalysis)
            .setOptLevel(optionsProcessor.optArgs.optLevel)
            .setSmallTypeIds(optionsProcessor.smallTypeIds)
            .setBackend(optionsProcessor.backend)
            .setGraphGenConfig(optionsProcessor.graphGenConfig)
            .setLlvmConfigurationBuilder(LLVMConfiguration.builder()
                .setEmitAssembly(optionsProcessor.emitAssembly)
                .setEmitIr(optionsProcessor.llvmArgs.emitIr)
                .setCompileOutput(optionsProcessor.compileOutput)
                .setPie(optionsProcessor.isPie)
                .setPlatform(platform)
                .setReferenceStrategy(platform.isWasm() ? ReferenceStrategy.POINTER : ReferenceStrategy.POINTER_AS1)
                .addLlcOptions(optionsProcessor.llvmArgs.llcOptions)
                .setStatepointEnabled(! platform.isWasm()))
            .setPlatform(platform)
            .addLibrarySearchPaths(splitPathString(System.getenv("LIBRARY_PATH")))
            .addLibrarySearchPaths(optionsProcessor.libSearchPaths);
        if (optionsProcessor.sourceOutputPath != null) {
            mainBuilder.setSourceOutputPath(optionsProcessor.sourceOutputPath);
        }

        Main main = mainBuilder.build();
        DiagnosticContext context = main.call();
        int errors = context.errors();
        int warnings = context.warnings();
        if (errors > 0) {
            if (warnings > 0) {
                System.err.printf("Compilation failed at %s with %d error(s) and %d warning(s)%n", Instant.now().atZone(ZoneId.systemDefault()), Integer.valueOf(errors), Integer.valueOf(warnings));
            } else {
                System.err.printf("Compilation failed at %s with %d error(s)%n", Instant.now().atZone(ZoneId.systemDefault()), Integer.valueOf(errors));
            }
        } else if (warnings > 0) {
            System.err.printf("Compilation completed at %s with %d warning(s)%n", Instant.now().atZone(ZoneId.systemDefault()), Integer.valueOf(warnings));
        }
        System.exit(errors == 0 ? 0 : 1);
    }

    private enum CmdResult {
        CMD_RESULT_HELP,
        CMD_RESULT_OK,
        CMD_RESULT_ERROR,
        ;
    }

    @CommandLine.Command(versionProvider = VersionProvider.class, mixinStandardHelpOptions = true)
    private static final class CommandLineProcessor {
        private enum GCType {
            SEMI("semi"),
            ;
            private final String gcType;

            GCType(String type) {
                this.gcType = type;
            }
            public String toString() {
                return gcType;
            }
        }

        @CommandLine.Option(names = "--boot-path-prepend-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void prependBootPathArtifact(List<ClassPathEntry.MavenArtifact> artifact) {
            prependedBootPathEntries.addAll(artifact);
        }
        @CommandLine.Option(names = "--boot-path-prepend-file", converter = ClassPathEntry.FilePath.Converter.class)
        void prependBootPathFile(List<ClassPathEntry.FilePath> filePath) {
            prependedBootPathEntries.addAll(filePath);
        }
        private final List<ClassPathEntry> prependedBootPathEntries = new ArrayList<>();

        @CommandLine.Option(names = "--boot-path-append-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void appendBootPathArtifact(List<ClassPathEntry.MavenArtifact> artifact) {
            appendedBootPathEntries.addAll(artifact);
        }
        @CommandLine.Option(names = "--boot-path-append-file", converter = ClassPathEntry.FilePath.Converter.class)
        void appendBootPathFile(List<ClassPathEntry.FilePath> filePath) {
            appendedBootPathEntries.addAll(filePath);
        }
        private final List<ClassPathEntry> appendedBootPathEntries = new ArrayList<>();

        @CommandLine.Option(names = "--rt-version")
        private String rtVersion = Version.CLASSLIB_DEFAULT_VERSION;

        @CommandLine.Option(names = "--app-path-artifact", converter = ClassPathEntry.MavenArtifact.Converter.class)
        void addAppPathArtifact(List<ClassPathEntry.MavenArtifact> artifact) {
            appPathEntries.addAll(artifact);
        }
        @CommandLine.Option(names = "--app-path-file", converter = ClassPathEntry.FilePath.Converter.class)
        void addAppPathFile(List<ClassPathEntry.FilePath> filePath) {
            appPathEntries.addAll(filePath);
        }
        private final List<ClassPathEntry> appPathEntries = new ArrayList<>();

        @CommandLine.Option(names="--qbicc-feature", description = "qbicc build configuration file")
        void addQbiccFeature(List<Path> features) {
            for (Path p:features) {
                try {
                    qbiccFeatures.add(p.toAbsolutePath().toUri().toURL());
                } catch (MalformedURLException e) {
                }
            }
        }
        private final Set<URL> qbiccFeatures = new HashSet<>();

        @CommandLine.Option(names = "--jar", description = "Compile an executable jar")
        private Path inputJar;

        @CommandLine.Option(names = "--output-path", description = "Specify directory where build files are placed")
        private Path outputPath;
        @CommandLine.Option(names = { "--output-name", "-o" }, defaultValue = "a.out", description = "Specify the name of the output executable file or library")
        private String outputName;
        @CommandLine.Option(names = "--no-compile-output", negatable = true, defaultValue = "true", description = "Enable/disable compilation of output files")
        boolean compileOutput;
        @CommandLine.Option(names = "--source-output-path", required = false, description = "Specify directory where sources for debugging are placed")
        private Path sourceOutputPath;
        @CommandLine.Option(names = "--debug")
        private boolean debug;
        @CommandLine.Option(names = "--debug-vtables")
        private boolean debugVTables;
        @CommandLine.Option(names = "--dispatch-stats")
        private boolean dispatchStats;
        @CommandLine.Option(names = "--debug-reachability")
        private boolean debugReachability;
        @CommandLine.Option(names = "--debug-supers")
        private boolean debugSupers;
        @CommandLine.Option(names = "--debug-devirt")
        private boolean debugDevirt;
        @CommandLine.Option(names = "--debug-interpreter")
        private boolean debugInterpreter;
        @CommandLine.Option(names = "--debug-initialization")
        private boolean debugInit;
        @CommandLine.Option(names = "--emit-dot", negatable = true, defaultValue = "false", description = "Enable emitting DOT graphs for each method")
        private boolean emitDot;
        @CommandLine.Option(names = "--emit-asm", negatable = true, defaultValue = "false", description = "Enable emitting assembly for each class")
        private boolean emitAssembly;
        @CommandLine.Option(names = "--gc", defaultValue = "semi", description = "Type of GC to use. Valid values: ${COMPLETION-CANDIDATES}")
        private GCType gc;
        @CommandLine.Option(names = "--heap-stats")
        private boolean heapStats;
        @CommandLine.Option(names = "--method-data-stats")
        private boolean methodDataStats;
        @CommandLine.Option(names = "--pie", negatable = true, defaultValue = "false", description = "[Disable|Enable] generation of position independent executable")
        private boolean isPie;
        @CommandLine.Option(names = "--platform", converter = PlatformConverter.class)
        private Platform platform;
        @CommandLine.Option(names = "--string-pool-stats")
        private boolean stringPoolStats;

        @CommandLine.Option(names = { "--library-search-path", "-L" }, description = "Additional library search paths")
        private List<Path> libSearchPaths;

        @CommandLine.Option(names = "--small-type-ids", negatable = true, defaultValue = "false", description = "Use narrow (16-bit) type ID values if true, wide (32-bit) type ID values if false")
        private boolean smallTypeIds;

        @CommandLine.Option(names = "--backend", defaultValue = "llvm", description = "The backend type to use. Valid values: ${COMPLETION-CANDIDATES}")
        private Backend backend;

        @CommandLine.Parameters(index="0", arity="1", defaultValue = "", description = "Application main class")
        private String mainClass;

        @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..1", heading = "Options for controlling generation of graphs for methods%n")
        private GraphGenArgs graphGenArgs;

        @CommandLine.ArgGroup(exclusive = false, heading = "Options for controlling optimizations%n")
        private OptArgs optArgs = new OptArgs();

        @CommandLine.ArgGroup(exclusive = false, heading = "Options for controlling the LLVM backend%n")
        private LLVMArgs llvmArgs = new LLVMArgs();

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
            @CommandLine.Option(names = "--no-opt-phis", negatable = true, defaultValue = "true", description = "Enable/disable `phi` elimination")
            boolean optPhis;
            @CommandLine.Option(names = "--no-opt-gotos", negatable = true, defaultValue = "true", description = "Enable/disable `goto` elimination")
            boolean optGotos;
            @CommandLine.Option(names = "--escape-analysis", negatable = true, defaultValue = "false", description = "Enable/disable escape analysis")
            boolean optEscapeAnalysis;
            @CommandLine.Option(names = { "-O", "--opt-level" }, defaultValue = "1", description = "Optimization level, between 0 and 3 (inclusive)")
            int optLevel;
        }

        static class LLVMArgs {
            @CommandLine.Option(names = "--emit-llvm-ir", negatable = true, defaultValue = "false", description = "Enable emitting LLVM IR for each class")
            boolean emitIr;
            @CommandLine.Option(names = "--llvm-llc-option", split = ",", description = "Pass options to the LLVM llc command")
            private List<String> llcOptions = new ArrayList<String>();
        }

        public CmdResult process(String[] args) {
            try {
                CommandLine commandLine = new CommandLine(this);
                ParseResult parseResult = commandLine.parseArgs(args);
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
            if (debugReachability) {
                Logger.getLogger("org.qbicc.plugin.reachability").setLevel(Level.DEBUG);
            }
            if (debugSupers) {
                Logger.getLogger("org.qbicc.plugin.instanceofcheckcast.supers").setLevel(Level.DEBUG);
            }
            if (debugDevirt) {
                Logger.getLogger("org.qbicc.plugin.dispatch.devirt").setLevel(Level.DEBUG);
            }
            if (debugInterpreter) {
                Logger.getLogger("org.qbicc.interpreter").setLevel(Level.DEBUG);
            }
            if (debugInit) {
                Logger.getLogger("org.qbicc.interpreter.initialization").setLevel(Level.DEBUG);
            }
            if (heapStats) {
                Logger.getLogger("org.qbicc.plugin.serialization.stats").setLevel(Level.DEBUG);
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
                graphGenConfig.setEnabled(true);
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
        private final List<ClassPathEntry> bootPathsPrepend = new ArrayList<>();
        private final List<ClassPathEntry> bootPathsAppend = new ArrayList<>();
        private final List<ClassPathEntry> appPaths = new ArrayList<>();
        private int optLevel;
        private String classLibVersion = Version.CLASSLIB_DEFAULT_VERSION;
        private Path outputPath;
        private String outputName = "a.out";
        private Path sourceOutputPath;
        private Consumer<Iterable<Diagnostic>> diagnosticsHandler = diagnostics -> {};
        private Platform platform = Platform.HOST_PLATFORM;
        private String mainClass;
        private List<String> buildTimeInitRootClasses = new ArrayList<>();
        private List<String> propertyDefines = new ArrayList<>(); // pairs of strings: property, value
        private String gc = "semi";
        // TODO Detect whether the system uses PIEs by default and match that if possible
        private boolean isPie = false;
        private boolean compileOutput = true;
        private boolean optMemoryTracking = false;
        private boolean optInlining = false;
        private boolean optPhis = true;
        private boolean optGotos = true;
        private boolean optEscapeAnalysis = false;
        private GraphGenConfig graphGenConfig;
        private boolean outputDot = false;
        private boolean smallTypeIds = false;
        private Backend backend = Backend.llvm;
        private List<Path> librarySearchPaths = List.of();
        private List<URL> qbiccYamlFeatures = new ArrayList<>();
        private List<QbiccFeature> qbiccFeatures = new ArrayList<>();
        private ClassPathResolver classPathResolver;
        private LLVMConfiguration.Builder llvmConfigurationBuilder;

        Builder() {}

        public Builder appendBootPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsAppend.add(entry);
            return this;
        }

        public Builder appendBootPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsAppend.addAll(entry);
            return this;
        }

        public Builder prependBootPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsPrepend.add(entry);
            return this;
        }

        public Builder prependBootPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            bootPathsPrepend.addAll(entry);
            return this;
        }

        public Builder setClassLibVersion(String classLibVersion) {
            Assert.checkNotNullParam("classLibVersion", classLibVersion);
            this.classLibVersion = classLibVersion;
            return this;
        }

        public Builder addAppPath(ClassPathEntry entry) {
            Assert.checkNotNullParam("entry", entry);
            appPaths.add(entry);
            return this;
        }

        public Builder addAppPaths(List<ClassPathEntry> entry) {
            Assert.checkNotNullParam("entry", entry);
            appPaths.addAll(entry);
            return this;
        }

        public Builder addQbiccYamlFeatures(List<URL> configs) {
            qbiccYamlFeatures.addAll(configs);
            return this;
        }

        public Builder addQbiccFeature(QbiccFeature feature) {
            qbiccFeatures.add(feature);
            return this;
        }

        public Builder addBuildTimeInitRootClass(String className) {
            buildTimeInitRootClasses.add(className);
            return this;
        }

        public Builder addPropertyDefine(String property, String value) {
            propertyDefines.add(property);
            propertyDefines.add(value);
            return this;
        }

        public Builder setOptLevel(int optLevel) {
            Assert.checkMinimumParameter("optLevel", 0, optLevel);
            Assert.checkMaximumParameter("optLevel", 3, optLevel);
            this.optLevel = optLevel;
            return this;
        }

        public Builder setOutputPath(Path path) {
            Assert.checkNotNullParam("path", path);
            this.outputPath = path;
            return this;
        }

        public Builder setOutputName(String outputName) {
            Assert.checkNotNullParam("outputName", outputName);
            this.outputName = outputName;
            return this;
        }

        public Builder setSourceOutputPath(Path path) {
            Assert.checkNotNullParam("path", path);
            this.sourceOutputPath = path;
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

        public Builder processJarArgument(Path inputJar) {
            if (inputJar == null) {
                return this;
            }

            try {
                JarInputStream jarStream = new JarInputStream(new FileInputStream(inputJar.toAbsolutePath().toString()));
                appPaths.add(ClassPathEntry.of(inputJar));
                mainClass = jarStream.getManifest().getMainAttributes().getValue("Main-Class");
                String classPath = jarStream.getManifest().getMainAttributes().getValue("Class-Path");
                if (classPath != null && !classPath.equals("")) {
                    Path parentDir = inputJar.toAbsolutePath().getParent();
                    for (String e : classPath.split(" ")) {
                        if (!e.isEmpty()) {
                            ClassPathEntry cpe = ClassPathEntry.of(parentDir.resolve(e));
                            appPaths.add(cpe);
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Error processing argument \"-jar "+inputJar+"\": "+e.getMessage());
            }
            return this;
        }

        public Builder setMainClass(String mainClass) {
            if (!mainClass.equals("")) {
                this.mainClass = mainClass;
            }
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

        public Builder setOutputDot(boolean outputDot) {
            this.outputDot = outputDot;
            return this;
        }

        public Builder setGraphGenConfig(GraphGenConfig graphGenConfig) {
            Assert.checkNotNullParam("graphGenConfig", graphGenConfig);
            this.graphGenConfig = graphGenConfig;
            return this;
        }

        public Builder setCompileOutput(boolean compileOutput) {
            this.compileOutput = compileOutput;
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

        public Builder setOptEscapeAnalysis(boolean optEscapeAnalysis) {
            this.optEscapeAnalysis = optEscapeAnalysis;
            return this;
        }

        public Builder setBackend(Backend backend) {
            this.backend = Assert.checkNotNullParam("backend", backend);
            return this;
        }

        public Builder setSmallTypeIds(boolean smallTypeIds) {
            this.smallTypeIds = smallTypeIds;
            return this;
        }

        public Builder addLibrarySearchPaths(List<Path> librarySearchPaths) {
            if (librarySearchPaths != null && !librarySearchPaths.isEmpty()) {
                if (this.librarySearchPaths.isEmpty()) {
                    this.librarySearchPaths = List.copyOf(librarySearchPaths);
                } else {
                    Path[] p1 = this.librarySearchPaths.toArray(Path[]::new);
                    Path[] p2 = librarySearchPaths.toArray(Path[]::new);
                    Path[] finalPaths = Arrays.copyOf(p1, p1.length + p2.length);
                    System.arraycopy(p2, 0, finalPaths, p1.length, p2.length);
                    this.librarySearchPaths = List.of(finalPaths);
                }
            }
            return this;
        }

        public Builder setClassPathResolver(ClassPathResolver classPathResolver) {
            this.classPathResolver = classPathResolver;
            return this;
        }

        public Builder setLlvmConfigurationBuilder(final LLVMConfiguration.Builder builder) {
            this.llvmConfigurationBuilder = Assert.checkNotNullParam("builder", builder);
            return this;
        }

        public Main build() {
            return new Main(this);
        }
    }

    static class VersionProvider implements CommandLine.IVersionProvider {
        @Override
        public String[] getVersion() {
            return new String[] { "Qbicc version " + Version.QBICC_VERSION };
        }
    }
}
