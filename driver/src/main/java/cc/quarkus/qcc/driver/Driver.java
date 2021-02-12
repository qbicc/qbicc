package cc.quarkus.qcc.driver;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.BasicBlock;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.ParameterValue;
import cc.quarkus.qcc.graph.Value;
import cc.quarkus.qcc.graph.ValueHandle;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.interpreter.Vm;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.machine.arch.Platform;
import cc.quarkus.qcc.machine.object.ObjectFileProvider;
import cc.quarkus.qcc.machine.tool.CToolChain;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.tool.llvm.LlvmToolChain;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.ModuleDefinition;
import cc.quarkus.qcc.type.definition.ResolvedTypeDefinition;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ElementVisitor;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FunctionElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;

/**
 * A simple driver to run all the stages of compilation.
 */
public class Driver implements Closeable {
    private static final Logger log = Logger.getLogger("cc.quarkus.qcc.driver");

    static final String MODULE_INFO = "module-info.class";

    public static final AttachmentKey<CToolChain> C_TOOL_CHAIN_KEY = new AttachmentKey<>();
    public static final AttachmentKey<LlvmToolChain> LLVM_TOOL_KEY = new AttachmentKey<>();
    public static final AttachmentKey<ObjectFileProvider> OBJ_PROVIDER_TOOL_KEY = new AttachmentKey<>();

    final BaseDiagnosticContext initialContext;
    final CompilationContextImpl compilationContext;
    // at this point, the phase is initialized to ADD
    final List<Consumer<? super CompilationContext>> preAddHooks;
    final List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories;
    final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> addBuilderFactory;
    final List<Consumer<? super CompilationContext>> postAddHooks;
    // at this point, the phase is switched to ANALYZE
    final List<Consumer<? super CompilationContext>> preAnalyzeHooks;
    final BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> addToAnalyzeCopiers;
    final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> analyzeBuilderFactory;
    final List<Consumer<? super CompilationContext>> postAnalyzeHooks;
    // at this point, the phase is switched to LOWER
    final List<Consumer<? super CompilationContext>> preLowerHooks;
    final BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> analyzeToLowerCopiers;
    final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> lowerBuilderFactory;
    final List<Consumer<? super CompilationContext>> postLowerHooks;
    // at this point, the phase is switched to GENERATE
    final List<Consumer<? super CompilationContext>> preGenerateHooks;
    final List<ElementVisitor<CompilationContext, Void>> generateVisitors;
    final List<Consumer<? super CompilationContext>> postGenerateHooks;
    final Map<String, BootModule> bootModules;
    final List<ClassPathElement> bootClassPath;
    final Path outputDir;

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

    Driver(final Builder builder) {
        initialContext = Assert.checkNotNullParam("builder.initialContext", builder.initialContext);
        outputDir = Assert.checkNotNullParam("builder.outputDirectory", builder.outputDirectory);
        typeBuilderFactories = builder.typeBuilderFactories;
        initialContext.putAttachment(C_TOOL_CHAIN_KEY, Assert.checkNotNullParam("builder.toolChain", builder.toolChain));
        initialContext.putAttachment(LLVM_TOOL_KEY, Assert.checkNotNullParam("builder.llvmToolChain", builder.llvmToolChain));
        initialContext.putAttachment(OBJ_PROVIDER_TOOL_KEY, Assert.checkNotNullParam("builder.objectFileProvider", builder.objectFileProvider));
        // type system
        final TypeSystem typeSystem = builder.typeSystem;
        final LiteralFactory literalFactory = LiteralFactory.create(typeSystem);

        // boot modules
        Map<String, BootModule> bootModules = new HashMap<>();
        List<ClassPathElement> bootClassPath = new ArrayList<>();
        for (Path path : builder.bootClassPathElements) {
            // open all bootstrap JARs (MR bootstrap JARs not supported)
            ClassPathElement element;
            if (Files.isDirectory(path)) {
                element = new DirectoryClassPathElement(path);
            } else {
                try {
                    element = new JarFileClassPathElement(new JarFile(path.toFile(), true, ZipFile.OPEN_READ));
                } catch (Exception e) {
                    initialContext.error("Failed to open boot class path JAR \"%s\": %s", path, e);
                    continue;
                }
            }
            bootClassPath.add(element);
            try (ClassPathElement.Resource moduleInfo = element.getResource(MODULE_INFO)) {
                ByteBuffer buffer = moduleInfo.getBuffer();
                if (buffer == null) {
                    // ignore non-module
                    continue;
                }
                ModuleDefinition moduleDefinition = ModuleDefinition.create(buffer);
                bootModules.put(moduleDefinition.getName(), new BootModule(element, moduleDefinition));
            } catch (Exception e) {
                initialContext.error("Failed to read module from class path element \"%s\": %s", path, e);
            }
        }
        BootModule javaBase = bootModules.get("java.base");
        if (javaBase == null) {
            initialContext.error("Bootstrap failed: no java.base module found");
        }
        this.bootModules = bootModules;
        this.bootClassPath = bootClassPath;

        // ADD phase
        preAddHooks = List.copyOf(builder.preHooks.getOrDefault(Phase.ADD, List.of()));
        // (no copiers)
        addBuilderFactory = constructFactory(builder, Phase.ADD);
        postAddHooks = List.copyOf(builder.postHooks.getOrDefault(Phase.ADD, List.of()));

        // ANALYZE phase
        preAnalyzeHooks = List.copyOf(builder.preHooks.getOrDefault(Phase.ANALYZE, List.of()));
        addToAnalyzeCopiers = constructCopiers(builder, Phase.ANALYZE);
        analyzeBuilderFactory = constructFactory(builder, Phase.ANALYZE);
        postAnalyzeHooks = List.copyOf(builder.postHooks.getOrDefault(Phase.ANALYZE, List.of()));

        // LOWER phase
        preLowerHooks = List.copyOf(builder.preHooks.getOrDefault(Phase.LOWER, List.of()));
        analyzeToLowerCopiers = constructCopiers(builder, Phase.LOWER);
        lowerBuilderFactory = constructFactory(builder, Phase.LOWER);
        postLowerHooks = List.copyOf(builder.postHooks.getOrDefault(Phase.LOWER, List.of()));

        // GENERATE phase
        preGenerateHooks = List.copyOf(builder.preHooks.getOrDefault(Phase.GENERATE, List.of()));
        generateVisitors = List.copyOf(builder.generateVisitors); // instead of a copier
        // (no builder factory)
        postGenerateHooks = List.copyOf(builder.postHooks.getOrDefault(Phase.GENERATE, List.of()));

        List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories = new ArrayList<>(builder.resolverFactories);
        Collections.reverse(resolverFactories);

        final BiFunction<VmObject, String, DefinedTypeDefinition> finder;
        Vm vm = builder.vm;
        if (vm != null) {
            finder = vm::loadClass;
        } else {
            // use a simple finder instead
            finder = this::defaultFinder;
        }

        compilationContext = new CompilationContextImpl(initialContext, typeSystem, literalFactory, finder, outputDir, resolverFactories);
        // start with ADD
        compilationContext.setBlockFactory(addBuilderFactory);
    }

    private static BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> constructCopiers(final Builder builder, final Phase phase) {
        List<BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>>> list = builder.copyFactories.getOrDefault(phase, List.of());
        if (list.isEmpty()) {
            return (c, v) -> v;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        ArrayList<BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>>> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return (c, v) -> {
            for (int i = 0; i < copy.size(); i ++) {
                v = copy.get(i).apply(c, v);
            }
            return v;
        };
    }

    private static BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> constructFactory(final Builder builder, final Phase phase) {
        BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder> addWrapper = assembleFactories(builder.builderFactories.getOrDefault(phase, Map.of()));
        return (ctxt, executableElement) -> addWrapper.apply(ctxt, BasicBlockBuilder.simpleBuilder(ctxt.getTypeSystem(), executableElement));
    }

    private static BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder> assembleFactories(Map<BuilderStage, List<BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder>>> map) {
        return assembleFactories(List.of(
            assembleFactories(map.getOrDefault(BuilderStage.TRANSFORM, List.of())),
            assembleFactories(map.getOrDefault(BuilderStage.CORRECT, List.of())),
            assembleFactories(map.getOrDefault(BuilderStage.OPTIMIZE, List.of())),
            assembleFactories(map.getOrDefault(BuilderStage.INTEGRITY, List.of()))
        ));
    }

    private static BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder> assembleFactories(List<BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder>> list) {
        if (list.isEmpty()) {
            return (c, b) -> b;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        List<BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder>> copy = new ArrayList<>(list);
        Collections.reverse(copy);
        return (c, builder) -> {
            for (int i = 0; i < copy.size(); i ++) {
                builder = copy.get(i).apply(c, builder);
            }
            return builder;
        };
    }

    private DefinedTypeDefinition defaultFinder(VmObject classLoader, String name) {
        if (classLoader != null) {
            return null;
        }
        String fileName = name + ".class";
        ByteBuffer buffer;
        ClassContext ctxt = compilationContext.getBootstrapClassContext();
        for (ClassPathElement element : bootClassPath) {
            try (ClassPathElement.Resource resource = element.getResource(fileName)) {
                buffer = resource.getBuffer();
                if (buffer == null) {
                    // non existent
                    continue;
                }
                ClassFile classFile = ClassFile.of(ctxt, buffer);
                DefinedTypeDefinition.Builder builder = DefinedTypeDefinition.Builder.basic();
                for (BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder> factory : typeBuilderFactories) {
                    builder = factory.apply(ctxt, builder);
                }
                classFile.accept(builder);
                DefinedTypeDefinition def = builder.build();
                ctxt.defineClass(name, def);
                return def;
            } catch (Exception e) {
                log.warnf(e, "An exception was thrown while loading class \"%s\" from the bootstrap loader", name);
                ctxt.getCompilationContext().warning("Failed to load class \"%s\" from the bootstrap loader due to an exception: %s", name, e);
                return null;
            }
        }
        return null;
    }

    public CompilationContext getCompilationContext() {
        return compilationContext;
    }

    private ResolvedTypeDefinition loadAndResolveBootstrapClass(String name) {
        DefinedTypeDefinition clazz = compilationContext.getBootstrapClassContext().findDefinedType(name);
        if (clazz == null) {
            compilationContext.error("Required bootstrap class \"%s\" was not found", name);
            return null;
        }
        try {
            return clazz.validate().resolve();
        } catch (Exception ex) {
            log.error("An exception was thrown while resolving a bootstrap class", ex);
            compilationContext.error("Failed to resolve bootstrap class \"%s\": %s", name, ex);
            return null;
        }
    }

    /**
     * Execute the compilation.
     *
     * @return {@code true} if compilation succeeded, {@code false} otherwise
     */
    public boolean execute() {
        CompilationContextImpl compilationContext = this.compilationContext;

        // ADD phase

        for (Consumer<? super CompilationContext> hook : preAddHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a pre-add hook", e);
                compilationContext.error(e, "Pre-add hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }
        ResolvedTypeDefinition stringClass = loadAndResolveBootstrapClass("java/lang/String");
        if (stringClass == null) {
            return false;
        }
        ResolvedTypeDefinition threadClass = loadAndResolveBootstrapClass("java/lang/Thread");
        if (threadClass == null) {
            return false;
        }
        ResolvedTypeDefinition vmClass = loadAndResolveBootstrapClass("cc/quarkus/qcc/runtime/main/VM");
        if (vmClass == null) {
            return false;
        }

        // trace out the program graph, enqueueing each item one time and then processing every item in the queue;
        // in this stage we're just loading everything that *might* be reachable

        for (ExecutableElement entryPoint : compilationContext.getEntryPoints()) {
            compilationContext.enqueue(entryPoint);
        }

        ExecutableElement element = compilationContext.dequeue();
        if (element != null) do {
            // todo: this will be removed once the VM is functional
            // make sure the initializer is enqueued
            InitializerElement initializer = element.getEnclosingType().validate().resolve().getInitializer();
            if (initializer != null) {
                compilationContext.enqueue(initializer);
            }
            if (element.hasMethodBody()) {
                // cause method and field references to be resolved
                try {
                    element.getOrCreateMethodBody();
                } catch (Exception e) {
                    log.error("An exception was thrown while constructing a method body", e);
                    compilationContext.error(element, "Exception while constructing method body: %s", e);
                }
            }
            element = compilationContext.dequeue();
        } while (element != null);

        if (compilationContext.errors() > 0) {
            // bail out
            return false;
        }

        for (Consumer<? super CompilationContext> hook : postAddHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a post-add hook", e);
                compilationContext.error("Post-add hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        if (compilationContext.errors() > 0) {
            // bail out
            return false;
        }

        compilationContext.clearEnqueuedSet();

        // ANALYZE phase

        compilationContext.setBlockFactory(analyzeBuilderFactory);

        for (Consumer<? super CompilationContext> hook : preAnalyzeHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a pre-analyze hook", e);
                compilationContext.error("Pre-analyze hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        // In this phase we start from the entry points again, and then copy (and filter) all of the nodes to a smaller reachable set

        for (ExecutableElement entryPoint : compilationContext.getEntryPoints()) {
            compilationContext.enqueue(entryPoint);
        }

        element = compilationContext.dequeue();
        if (element != null) do {
            // todo: this will be removed once the VM is functional
            // make sure the initializer is enqueued
            InitializerElement initializer = element.getEnclosingType().validate().resolve().getInitializer();
            if (initializer != null) {
                compilationContext.enqueue(initializer);
            }
            if (element.hasMethodBody()) {
                // rewrite the method body
                ClassContext classContext = element.getEnclosingType().getContext();
                MethodBody original = element.getOrCreateMethodBody();
                BasicBlock entryBlock = original.getEntryBlock();
                BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
                BasicBlock copyBlock = Node.Copier.execute(entryBlock, builder, compilationContext, addToAnalyzeCopiers);
                builder.finish();
                element.replaceMethodBody(MethodBody.of(copyBlock, Schedule.forMethod(copyBlock), original.getThisValue(), original.getParameterValues()));
            }
            element = compilationContext.dequeue();
        } while (element != null);

        if (compilationContext.errors() > 0) {
            // bail out
            return false;
        }

        for (Consumer<? super CompilationContext> hook : postAnalyzeHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a post-analyze hook", e);
                compilationContext.error("Post-analyze hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        compilationContext.clearEnqueuedSet();

        // LOWER phase

        compilationContext.setBlockFactory(lowerBuilderFactory);

        for (Consumer<? super CompilationContext> hook : preLowerHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a pre-lower hook", e);
                compilationContext.error("Pre-lower hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        // start from entry points one more time, and copy the method bodies to their corresponding function body

        for (ExecutableElement entryPoint : compilationContext.getEntryPoints()) {
            compilationContext.enqueue(entryPoint);
        }

        element = compilationContext.dequeue();
        while (element != null) {
            if (element.hasMethodBody()) {
                // copy to a function; todo: this should eventually be done in the lowering plugin
                ClassContext classContext = element.getEnclosingType().getContext();
                MethodBody original = element.getMethodBody();
                BasicBlock entryBlock = original.getEntryBlock();
                List<ParameterValue> paramValues;
                ParameterValue thisValue;
                BasicBlockBuilder builder = classContext.newBasicBlockBuilder(element);
                if (element instanceof FunctionElement) {
                    paramValues = original.getParameterValues();
                    thisValue = null;
                } else {
                    List<ParameterValue> origParamValues = original.getParameterValues();
                    paramValues = new ArrayList<>(origParamValues.size() + 2);
                    paramValues.add(builder.parameter(threadClass.getClassType().getReference(), "thr", 0));
                    if (! element.isStatic()) {
                        thisValue = original.getThisValue();
                        paramValues.add(thisValue);
                    } else {
                        thisValue = null;
                    }
                    paramValues.addAll(origParamValues);
                }
                Function function = compilationContext.getExactFunction(element);
                BasicBlock copyBlock = Node.Copier.execute(entryBlock, builder, compilationContext, analyzeToLowerCopiers);
                builder.finish();
                function.replaceBody(MethodBody.of(copyBlock, Schedule.forMethod(copyBlock), thisValue, paramValues));
            }
            element = compilationContext.dequeue();
        }

        if (compilationContext.errors() > 0) {
            // bail out
            return false;
        }

        for (Consumer<? super CompilationContext> hook : postLowerHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a post-lower hook", e);
                compilationContext.error("Post-lower hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        compilationContext.clearEnqueuedSet();

        // GENERATE phase

        for (Consumer<? super CompilationContext> hook : preGenerateHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a pre-generate hook", e);
                compilationContext.error("Pre-generate hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        // Visit each reachable node to build the executable program

        for (ExecutableElement entryPoint : compilationContext.getEntryPoints()) {
            compilationContext.enqueue(entryPoint);
        }

        List<ElementVisitor<CompilationContext, Void>> generateVisitors = this.generateVisitors;

        element = compilationContext.dequeue();
        while (element != null) {
            for (ElementVisitor<CompilationContext, Void> elementVisitor : generateVisitors) {
                try {
                    element.accept(elementVisitor, compilationContext);
                } catch (Exception e) {
                    log.error("An exception was thrown in an element visitor", e);
                    compilationContext.error(element, "Element visitor threw an exception: %s", e);
                }
            }
            element = compilationContext.dequeue();
        }

        if (compilationContext.errors() > 0) {
            // bail out
            return false;
        }

        // Finalize

        for (Consumer<? super CompilationContext> hook : postGenerateHooks) {
            try {
                hook.accept(compilationContext);
            } catch (Exception e) {
                log.error("An exception was thrown in a post-generate hook", e);
                compilationContext.error("Post-generate hook failed: %s", e);
            }
            if (compilationContext.errors() > 0) {
                // bail out
                return false;
            }
        }

        return compilationContext.errors() == 0;
    }

    public void close() {
        for (ClassPathElement element : bootClassPath) {
            try {
                element.close();
            } catch (IOException e) {
                compilationContext.warning(Location.builder().setSourceFilePath(element.getName()).build(), "Failed to close boot class path element: %s", e);
            }
        }
    }

    /**
     * Construct a new builder.
     *
     * @return the new builder (not {@code null})
     */
    public static Builder builder() {
        return new Builder();
    }

    public Iterable<Diagnostic> getDiagnostics() {
        return initialContext.getDiagnostics();
    }

    public static final class Builder {
        final List<Path> bootClassPathElements = new ArrayList<>();
        final Map<Phase, Map<BuilderStage, List<BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder>>>> builderFactories = new EnumMap<>(Phase.class);
        final Map<Phase, List<BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>>>> copyFactories = new EnumMap<>(Phase.class);
        final List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories = new ArrayList<>();
        final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories = new ArrayList<>();
        final Map<Phase, List<Consumer<? super CompilationContext>>> preHooks = new EnumMap<>(Phase.class);
        final Map<Phase, List<Consumer<? super CompilationContext>>> postHooks = new EnumMap<>(Phase.class);
        final List<ElementVisitor<CompilationContext, Void>> generateVisitors = new ArrayList<>();

        Path outputDirectory = Path.of(".");
        BaseDiagnosticContext initialContext;
        Platform targetPlatform;
        TypeSystem typeSystem;
        Vm vm;
        CToolChain toolChain;
        LlvmToolChain llvmToolChain;
        ObjectFileProvider objectFileProvider;

        String mainClass;

        Builder() {}

        public Builder setInitialContext(BaseDiagnosticContext initialContext) {
            this.initialContext = Assert.checkNotNullParam("initialContext", initialContext);
            return this;
        }

        public Builder addBootClassPathElement(Path path) {
            if (path != null) {
                bootClassPathElements.add(path);
            }
            return this;
        }

        public Builder addBootClassPathElements(List<Path> paths) {
            if (paths != null) {
                bootClassPathElements.addAll(paths);
            }
            return this;
        }

        public String getMainClass() {
            return mainClass;
        }

        public Builder setMainClass(final String mainClass) {
            this.mainClass = Assert.checkNotNullParam("mainClass", mainClass);
            return this;
        }

        public Builder addResolverFactory(BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver> factory) {
            resolverFactories.add(factory);
            return this;
        }

        private static <V> EnumMap<BuilderStage, V> newBuilderStageMap(Object key) {
            return new EnumMap<BuilderStage, V>(BuilderStage.class);
        }

        private static <E> ArrayList<E> newArrayList(Object key) {
            return new ArrayList<>();
        }

        public Builder addBuilderFactory(Phase phase, BuilderStage stage, BiFunction<? super CompilationContext, BasicBlockBuilder, BasicBlockBuilder> factory) {
            Assert.checkNotNullParam("phase", phase);
            Assert.checkNotNullParam("stage", stage);
            Assert.checkNotNullParam("factory", factory);
            builderFactories.computeIfAbsent(phase, Builder::newBuilderStageMap).computeIfAbsent(stage, Builder::newArrayList).add(factory);
            return this;
        }

        public Builder addCopyFactory(Phase phase, BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> factory) {
            Assert.checkNotNullParam("phase", phase);
            Assert.checkNotNullParam("factory", factory);
            copyFactories.computeIfAbsent(phase, Builder::newArrayList).add(factory);
            return this;
        }

        public Builder addTypeBuilderFactory(BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder> factory) {
            typeBuilderFactories.add(Assert.checkNotNullParam("factory", factory));
            return this;
        }

        public Builder addPreHook(Phase phase, Consumer<? super CompilationContext> hook) {
            if (hook != null) {
                Assert.checkNotNullParam("phase", phase);
                preHooks.computeIfAbsent(phase, Builder::newArrayList).add(hook);
            }
            return this;
        }

        public Builder addPostHook(Phase phase, Consumer<? super CompilationContext> hook) {
            if (hook != null) {
                Assert.checkNotNullParam("phase", phase);
                postHooks.computeIfAbsent(phase, Builder::newArrayList).add(hook);
            }
            return this;
        }

        public Builder addGenerateVisitor(ElementVisitor<CompilationContext, Void> visitor) {
            generateVisitors.add(Assert.checkNotNullParam("visitor", visitor));
            return this;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Builder setOutputDirectory(final Path outputDirectory) {
            this.outputDirectory = Assert.checkNotNullParam("outputDirectory", outputDirectory);
            return this;
        }

        public Platform getTargetPlatform() {
            return targetPlatform;
        }

        public Builder setTargetPlatform(final Platform targetPlatform) {
            this.targetPlatform = targetPlatform;
            return this;
        }

        public TypeSystem getTypeSystem() {
            return typeSystem;
        }

        public Builder setTypeSystem(final TypeSystem typeSystem) {
            this.typeSystem = typeSystem;
            return this;
        }

        public Vm getVm() {
            return vm;
        }

        public Builder setVm(final Vm vm) {
            this.vm = vm;
            return this;
        }

        public CToolChain getToolChain() {
            return toolChain;
        }

        public Builder setToolChain(final CToolChain toolChain) {
            this.toolChain = toolChain;
            return this;
        }

        public LlvmToolChain getLlvmToolChain() {
            return llvmToolChain;
        }

        public Builder setLlvmToolChain(final LlvmToolChain llvmToolChain) {
            this.llvmToolChain = llvmToolChain;
            return this;
        }

        public ObjectFileProvider getObjectFileProvider() {
            return objectFileProvider;
        }

        public Builder setObjectFileProvider(final ObjectFileProvider objectFileProvider) {
            this.objectFileProvider = objectFileProvider;
            return this;
        }

        public Driver build() {
            return new Driver(this);
        }
    }

    static final class BootModule implements Closeable {
        private final ClassPathElement element;
        private final ModuleDefinition moduleDefinition;

        BootModule(final ClassPathElement element, final ModuleDefinition moduleDefinition) {
            this.element = element;
            this.moduleDefinition = moduleDefinition;
        }

        public void close() throws IOException {
            element.close();
        }
    }
}
