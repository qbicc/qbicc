package cc.quarkus.qcc.main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.regex.Pattern;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
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
import cc.quarkus.qcc.plugin.conversion.NumericalConversionBasicBlockBuilder;
import cc.quarkus.qcc.plugin.correctness.ArrayIndexOutOfBoundsCheckingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.correctness.NullCheckingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.correctness.ShiftDistanceMaskingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.correctness.ZeroDivisorCheckingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.dispatch.DevirtualizingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.dispatch.VTableBuilder;
import cc.quarkus.qcc.plugin.dot.DotGenerator;
import cc.quarkus.qcc.plugin.intrinsics.IntrinsicBasicBlockBuilder;
import cc.quarkus.qcc.plugin.intrinsics.core.CoreIntrinsics;
import cc.quarkus.qcc.plugin.layout.ObjectAccessLoweringBuilder;
import cc.quarkus.qcc.plugin.layout.Layout;
import cc.quarkus.qcc.plugin.linker.LinkStage;
import cc.quarkus.qcc.plugin.llvm.LLVMCompileStage;
import cc.quarkus.qcc.plugin.llvm.LLVMGenerator;
import cc.quarkus.qcc.plugin.lowering.InvocationLoweringBasicBlockBuilder;
import cc.quarkus.qcc.plugin.lowering.StaticFieldLoweringBasicBlockBuilder;
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
import cc.quarkus.qcc.plugin.opt.GotoRemovingVisitor;
import cc.quarkus.qcc.plugin.opt.PhiOptimizerVisitor;
import cc.quarkus.qcc.plugin.opt.SimpleOptBasicBlockBuilder;
import cc.quarkus.qcc.plugin.reachability.RTAInfo;
import cc.quarkus.qcc.plugin.reachability.ReachabilityBlockBuilder;
import cc.quarkus.qcc.plugin.trycatch.LocalThrowHandlingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.trycatch.SynchronizedMethodBasicBlockBuilder;
import cc.quarkus.qcc.plugin.trycatch.ThrowValueBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.ClassLoadingBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.LowerVerificationBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.MemberResolvingBasicBlockBuilder;
import cc.quarkus.qcc.tool.llvm.LlvmToolChain;
import cc.quarkus.qcc.type.TypeSystem;

/**
 * The main entry point.
 */
public class Main {
    public static void main(String[] args) {
        final BaseDiagnosticContext initialContext = new BaseDiagnosticContext();
        final Driver.Builder builder = Driver.builder();
        builder.setInitialContext(initialContext);
        final Iterator<String> argIter = List.of(args).iterator();
        String mainClass = null;
        Path outputPath = null;
        while (argIter.hasNext()) {
            final String arg = argIter.next();
            if (arg.startsWith("-")) {
                if (arg.equals("--boot-module-path")) {
                    String[] path = argIter.next().split(Pattern.quote(File.pathSeparator));
                    for (String pathStr : path) {
                        if (! pathStr.isEmpty()) {
                            builder.addBootClassPathElement(Path.of(pathStr));
                        }
                    }
                } else if (arg.equals("--output-path") || arg.equals("-o")) {
                    outputPath = Path.of(argIter.next());
                } else {
                    initialContext.error("Unrecognized argument \"%s\"", arg);
                    break;
                }
            } else if (mainClass == null) {
                mainClass = arg;
            } else {
                initialContext.error("Extra argument \"%s\"", arg);
                break;
            }
        }
        if (mainClass == null) {
            initialContext.error("No main class specified");
        }
        if (outputPath == null) {
            initialContext.error("No output path specified");
        }
        int errors = initialContext.errors();
        if (errors == 0) {
            builder.setOutputDirectory(outputPath);
            // first, probe the target platform
            Platform target = Platform.HOST_PLATFORM;
            builder.setTargetPlatform(target);
            Optional<ObjectFileProvider> optionalProvider = ObjectFileProvider.findProvider(target.getObjectType(), Main.class.getClassLoader());
            if (optionalProvider.isEmpty()) {
                initialContext.error("No object file provider found for %s", target.getObjectType());
                errors = initialContext.errors();
            } else {
                ObjectFileProvider objectFileProvider = optionalProvider.get();
                Iterator<CToolChain> toolChains = CToolChain.findAllCToolChains(target, t -> true, Main.class.getClassLoader()).iterator();
                if (! toolChains.hasNext()) {
                    initialContext.error("No working C compiler found");
                    errors = initialContext.errors();
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
                            errors = initialContext.errors();
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

                                builder.addResolverFactory(ConstTypeResolver::new);
                                builder.addResolverFactory(FunctionTypeResolver::new);
                                builder.addResolverFactory(PointerTypeResolver::new);
                                builder.addResolverFactory(NativeTypeResolver::new);

                                builder.addPreHook(Phase.ADD, CoreIntrinsics::register);
                                builder.addPreHook(Phase.ADD, Layout::get);
                                builder.addPreHook(Phase.ADD, new AddMainClassHook());
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, IntrinsicBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, CloneConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, LocalThrowHandlingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ClassLoadingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantDefiningBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ConstantBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, NativeBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, MemberResolvingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, ThrowValueBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.TRANSFORM, SynchronizedMethodBasicBlockBuilder::createIfNeeded);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, ArrayIndexOutOfBoundsCheckingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, NullCheckingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, ShiftDistanceMaskingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.CORRECT, ZeroDivisorCheckingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ADD, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.ADD, RTAInfo::clear);

                                builder.addCopyFactory(Phase.ANALYZE, GotoRemovingVisitor::new);
                                builder.addCopyFactory(Phase.ANALYZE, PhiOptimizerVisitor::new);

                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.CORRECT, NumericalConversionBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.ANALYZE, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.ANALYZE, new VTableBuilder());
                                builder.addPostHook(Phase.ANALYZE, RTAInfo::clear);

                                builder.addCopyFactory(Phase.LOWER, GotoRemovingVisitor::new);

                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, DevirtualizingBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, InvocationLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, StaticFieldLoweringBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.TRANSFORM, ObjectAccessLoweringBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);
                                builder.addBuilderFactory(Phase.LOWER, BuilderStage.INTEGRITY, ReachabilityBlockBuilder::new);
                                builder.addPostHook(Phase.LOWER, RTAInfo::clear);

                                builder.addPreHook(Phase.GENERATE, new LLVMGenerator());

                                builder.addGenerateVisitor(DotGenerator.genPhase());

                                builder.addPostHook(Phase.GENERATE, new LLVMCompileStage());
                                builder.addPostHook(Phase.GENERATE, new LinkStage());

                                CompilationContext ctxt;
                                try (Driver driver = builder.build()) {
                                    ctxt = driver.getCompilationContext();
                                    MainMethod.get(ctxt).setMainClass(mainClass);
                                    driver.execute();
                                }
                                errors = ctxt.errors();
                            }
                        }
                    } catch (IOException e) {
                        initialContext.error(e, "Failed to probe system types from tool chain");
                        errors = initialContext.errors();
                    }
                }
            }
        }
        for (Diagnostic diagnostic : initialContext.getDiagnostics()) {
            try {
                diagnostic.appendTo(System.err);
            } catch (IOException e) {
                // just give up
                break;
            }
        }
        int warnings = initialContext.warnings();
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
}
