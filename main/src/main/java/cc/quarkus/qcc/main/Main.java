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
import cc.quarkus.qcc.driver.plugin.DriverPlugin;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.probe.CTypeProbe;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.plugin.llvm.LLVMGenerator;
import cc.quarkus.qcc.plugin.opt.PhiOptimizerVisitor;
import cc.quarkus.qcc.plugin.opt.SimpleOptBasicBlockBuilder;
import cc.quarkus.qcc.plugin.verification.LowerVerificationBasicBlockBuilder;
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
                    CTypeProbe.Builder probeBuilder = CTypeProbe.builder();
                    probeBuilder.addInclude("<stdint.h>");
                    // size and signedness of char
                    CTypeProbe.Type char_t = CTypeProbe.Type.builder().setName("char").build();
                    probeBuilder.addType(char_t);
                    // int sizes
                    CTypeProbe.Type int8_t = CTypeProbe.Type.builder().setName("int8_t").build();
                    probeBuilder.addType(int8_t);
                    CTypeProbe.Type int16_t = CTypeProbe.Type.builder().setName("int16_t").build();
                    probeBuilder.addType(int16_t);
                    CTypeProbe.Type int32_t = CTypeProbe.Type.builder().setName("int32_t").build();
                    probeBuilder.addType(int32_t);
                    CTypeProbe.Type int64_t = CTypeProbe.Type.builder().setName("int64_t").build();
                    probeBuilder.addType(int64_t);
                    // float sizes
                    CTypeProbe.Type float_t = CTypeProbe.Type.builder().setName("float").build();
                    probeBuilder.addType(float_t);
                    CTypeProbe.Type double_t = CTypeProbe.Type.builder().setName("double").build();
                    probeBuilder.addType(double_t);
                    // bool
                    CTypeProbe.Type _Bool = CTypeProbe.Type.builder().setName("_Bool").build();
                    probeBuilder.addType(_Bool);
                    // pointer
                    CTypeProbe.Type void_p = CTypeProbe.Type.builder().setName("void *").build();
                    probeBuilder.addType(void_p);
                    // execute
                    CTypeProbe probe = probeBuilder.build();
                    try {
                        CTypeProbe.Result probeResult = probe.run(toolChain, objectFileProvider);
                        long charSize = probeResult.getInfo(char_t).getSize();
                        if (charSize != 1) {
                            initialContext.error("Unexpected size of `char`: %d", Long.valueOf(charSize));
                        }
                        TypeSystem.Builder tsBuilder = TypeSystem.builder();
                        tsBuilder.setBoolSize((int) probeResult.getInfo(_Bool).getSize());
                        tsBuilder.setBoolAlignment((int) probeResult.getInfo(_Bool).getAlign());
                        tsBuilder.setByteBits(8); // TODO: add a constant probe for BYTE_BITS
                        tsBuilder.setInt8Size((int) probeResult.getInfo(int8_t).getSize());
                        tsBuilder.setInt8Alignment((int) probeResult.getInfo(int8_t).getAlign());
                        tsBuilder.setInt16Size((int) probeResult.getInfo(int16_t).getSize());
                        tsBuilder.setInt16Alignment((int) probeResult.getInfo(int16_t).getAlign());
                        tsBuilder.setInt32Size((int) probeResult.getInfo(int32_t).getSize());
                        tsBuilder.setInt32Alignment((int) probeResult.getInfo(int32_t).getAlign());
                        tsBuilder.setInt64Size((int) probeResult.getInfo(int64_t).getSize());
                        tsBuilder.setInt64Alignment((int) probeResult.getInfo(int64_t).getAlign());
                        tsBuilder.setFloat32Size((int) probeResult.getInfo(float_t).getSize());
                        tsBuilder.setFloat32Alignment((int) probeResult.getInfo(float_t).getAlign());
                        tsBuilder.setFloat64Size((int) probeResult.getInfo(double_t).getSize());
                        tsBuilder.setFloat64Alignment((int) probeResult.getInfo(double_t).getAlign());
                        tsBuilder.setPointerSize((int) probeResult.getInfo(void_p).getSize());
                        tsBuilder.setPointerAlignment((int) probeResult.getInfo(void_p).getAlign());
                        // todo: function alignment probe
                        // for now, references == pointers
                        tsBuilder.setReferenceSize((int) probeResult.getInfo(void_p).getSize());
                        tsBuilder.setReferenceAlignment((int) probeResult.getInfo(void_p).getAlign());
                        // for now, type IDs == int32
                        tsBuilder.setTypeIdSize((int) probeResult.getInfo(int32_t).getSize());
                        tsBuilder.setTypeIdAlignment((int) probeResult.getInfo(int32_t).getAlign());
                        builder.setTypeSystem(tsBuilder.build());
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
                            assert mainClass != null; // else errors would be != 0
                            // keep it simple to start with
                            builder.setMainClass(mainClass.replace('.', '/'));
                            builder.addPostAnalyticHook(new LLVMGenerator());
                            builder.addAdditivePhaseBlockBuilderFactory(BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                            builder.addCopyFactory(PhiOptimizerVisitor::new);
                            builder.addAnalyticPhaseBlockBuilderFactory(BuilderStage.OPTIMIZE, SimpleOptBasicBlockBuilder::new);
                            builder.addAnalyticPhaseBlockBuilderFactory(BuilderStage.INTEGRITY, LowerVerificationBasicBlockBuilder::new);
                            CompilationContext ctxt;
                            boolean result;
                            try (Driver driver = builder.build()) {
                                ctxt = driver.getCompilationContext();
                                driver.execute();
                            }
                            errors = ctxt.errors();
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
