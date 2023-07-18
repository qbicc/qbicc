package org.qbicc.driver;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.Locatable;
import org.qbicc.context.Location;
import org.qbicc.context.PhaseAttachmentKey;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.schedule.Scheduler;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.ModuleSection;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.object.Segment;
import org.qbicc.type.StructType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.InstanceMethodType;
import org.qbicc.type.InvokableType;
import org.qbicc.type.MethodType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.MethodTypeId;
import org.qbicc.type.definition.NativeMethodConfigurator;
import org.qbicc.type.definition.TypeId;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;
import org.qbicc.type.generic.ClassSignature;

final class CompilationContextImpl implements CompilationContext {
    private static final Logger log = Logger.getLogger("org.qbicc.driver");

    private final Platform platform;
    private final TypeSystem typeSystem;
    private final LiteralFactory literalFactory;
    private final BaseDiagnosticContext baseDiagnosticContext;
    private final ConcurrentMap<VmClassLoader, ClassContext> classLoaderContexts = new ConcurrentHashMap<>();
    final Queue<Object> queue = new ArrayDeque<>();
    final Set<ExecutableElement> entryPoints = ConcurrentHashMap.newKeySet();
    final ClassContext bootstrapClassContext;
    final Function<VmClassLoader, ClassContext> platformClassContextFactory;
    final Function<VmClassLoader, ClassContext> appClassContextFactory;
    private final ConcurrentMap<DefinedTypeDefinition, ProgramModule> programModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<ExecutableElement, org.qbicc.object.Function> exactFunctions = new ConcurrentHashMap<>();
    private final ConcurrentMap<ExecutableElement, FunctionElement> establishedFunctions = new ConcurrentHashMap<>();
    private final ConcurrentMap<TypeId, ConcurrentMap<List<TypeId>, MethodTypeId>> methodTypes = new ConcurrentHashMap<>();
    private final Path outputDir;
    final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories;
    private volatile DefinedTypeDefinition defaultTypeDefinition;
    private final List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories;

    // mutable state
    private volatile BiFunction<BasicBlockBuilder.FactoryContext, ExecutableElement, BasicBlockBuilder> blockFactory;
    private volatile BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> copier;
    private final Vm vm;
    private final NativeMethodConfigurator nativeMethodConfigurator;
    private final Consumer<ClassContext> classContextListener;
    private final Section implicitSection;
    private final Scheduler scheduler;

    CompilationContextImpl(final Builder builder) {
        this.baseDiagnosticContext = builder.baseDiagnosticContext;
        this.platform = builder.platform;
        this.typeSystem = builder.typeSystem;
        this.literalFactory = builder.literalFactory;
        this.scheduler = builder.scheduler;
        this.outputDir = builder.outputDir;
        this.resolverFactories = builder.resolverFactories;
        this.classContextListener = builder.classContextListener;
        bootstrapClassContext = new ClassContextImpl(this, null, builder.bootstrapFinder, builder.bootstrapResourceFinder, builder.bootstrapResourcesFinder);
        appClassContextFactory = cl -> new ClassContextImpl(this, cl, builder.appFinder, builder.appResourceFinder, builder.appResourcesFinder);
        platformClassContextFactory = cl -> new ClassContextImpl(this, cl, builder.platformFinder, builder.platformResourceFinder, builder.platformResourcesFinder);
        this.blockFactory = builder.initialBlockFactory;
        this.typeBuilderFactories = builder.typeBuilderFactories;
        this.nativeMethodConfigurator = builder.nativeMethodConfigurator;
        implicitSection = Section.defineSection(this, 0, IMPLICIT_SECTION_NAME, Segment.DATA);
        handleNewClassContext(bootstrapClassContext);
        // last!
        this.vm = builder.vmFactory.apply(this);
    }

    public <T> T getAttachment(final AttachmentKey<T> key) {
        return baseDiagnosticContext.getAttachment(key);
    }

    public <T> T getAttachmentOrDefault(final AttachmentKey<T> key, final T defVal) {
        return baseDiagnosticContext.getAttachmentOrDefault(key, defVal);
    }

    public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
        return baseDiagnosticContext.putAttachment(key, value);
    }

    public <T> T putAttachmentIfAbsent(final AttachmentKey<T> key, final T value) {
        return baseDiagnosticContext.putAttachmentIfAbsent(key, value);
    }

    public <T> T removeAttachment(final AttachmentKey<T> key) {
        return baseDiagnosticContext.removeAttachment(key);
    }

    public <T> boolean removeAttachment(final AttachmentKey<T> key, final T expect) {
        return baseDiagnosticContext.removeAttachment(key, expect);
    }

    public <T> T replaceAttachment(final AttachmentKey<T> key, final T update) {
        return baseDiagnosticContext.replaceAttachment(key, update);
    }

    public <T> boolean replaceAttachment(final AttachmentKey<T> key, final T expect, final T update) {
        return baseDiagnosticContext.replaceAttachment(key, expect, update);
    }

    public <T> T computeAttachmentIfAbsent(final AttachmentKey<T> key, final Supplier<T> function) {
        return baseDiagnosticContext.computeAttachmentIfAbsent(key, function);
    }

    public <T> T computeAttachmentIfPresent(final AttachmentKey<T> key, final Function<T, T> function) {
        return baseDiagnosticContext.computeAttachmentIfPresent(key, function);
    }

    public <T> T computeAttachment(final AttachmentKey<T> key, final Function<T, T> function) {
        return baseDiagnosticContext.computeAttachment(key, function);
    }

    @Override
    public <T> T getAttachment(PhaseAttachmentKey<T> key) {
        return baseDiagnosticContext.getAttachment(key);
    }

    @Override
    public <T> T getAttachmentOrDefault(PhaseAttachmentKey<T> key, T defVal) {
        return baseDiagnosticContext.getAttachmentOrDefault(key, defVal);
    }

    @Override
    public <T> T putAttachment(PhaseAttachmentKey<T> key, T value) {
        return baseDiagnosticContext.putAttachment(key, value);
    }

    @Override
    public <T> T putAttachmentIfAbsent(PhaseAttachmentKey<T> key, T value) {
        return baseDiagnosticContext.putAttachmentIfAbsent(key, value);
    }

    @Override
    public <T> T removeAttachment(PhaseAttachmentKey<T> key) {
        return baseDiagnosticContext.removeAttachment(key);
    }

    @Override
    public <T> boolean removeAttachment(PhaseAttachmentKey<T> key, T expect) {
        return baseDiagnosticContext.removeAttachment(key, expect);
    }

    @Override
    public <T> T replaceAttachment(PhaseAttachmentKey<T> key, T update) {
        return baseDiagnosticContext.replaceAttachment(key, update);
    }

    @Override
    public <T> boolean replaceAttachment(PhaseAttachmentKey<T> key, T expect, T update) {
        return baseDiagnosticContext.replaceAttachment(key, expect, update);
    }

    @Override
    public <T> T computeAttachmentIfAbsent(PhaseAttachmentKey<T> key, Supplier<T> function) {
        return baseDiagnosticContext.computeAttachmentIfAbsent(key, function);
    }

    @Override
    public <T> T computeAttachmentIfPresent(PhaseAttachmentKey<T> key, Function<T, T> function) {
        return baseDiagnosticContext.computeAttachmentIfPresent(key, function);
    }

    @Override
    public <T> T computeAttachment(PhaseAttachmentKey<T> key, Function<T, T> function) {
        return baseDiagnosticContext.computeAttachment(key, function);
    }

    @Override
    public void cyclePhaseAttachments() {
        baseDiagnosticContext.cyclePhaseAttachments();
    }

    @Override
    public <T> T getPreviousPhaseAttachment(PhaseAttachmentKey<T> key) {
        return baseDiagnosticContext.getPreviousPhaseAttachment(key);
    }

    public int errors() {
        return baseDiagnosticContext.errors();
    }

    public int warnings() {
        return baseDiagnosticContext.warnings();
    }

    public Diagnostic msg(final Diagnostic diagnostic) {
        return baseDiagnosticContext.msg(diagnostic);
    }

    public Diagnostic msg(final Diagnostic parent, final Location loc, final Diagnostic.Level level, final String fmt, final Object... args) {
        return baseDiagnosticContext.msg(parent, loc, level, fmt, args);
    }

    public Diagnostic msg(final Diagnostic parent, final Element element, final Node node, final Diagnostic.Level level, final String fmt, final Object... args) {
        return baseDiagnosticContext.msg(parent, element, node, level, fmt, args);
    }

    public Iterable<Diagnostic> getDiagnostics() {
        return baseDiagnosticContext.getDiagnostics();
    }

    public Platform getPlatform() {
        return platform;
    }

    public TypeSystem getTypeSystem() {
        return typeSystem;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public LiteralFactory getLiteralFactory() {
        return literalFactory;
    }

    public ClassContext getBootstrapClassContext() {
        return bootstrapClassContext;
    }

    @Override
    public ClassContext getClassContextForLoader(VmClassLoader classLoaderObject) {
        return classLoaderObject == null ? bootstrapClassContext : classLoaderContexts.get(classLoaderObject);
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        return baseDiagnosticContext.deduplicate(buffer, offset, length);
    }

    public String deduplicate(final String original) {
        return baseDiagnosticContext.deduplicate(original);
    }

    public ClassContext constructClassContext(final VmClassLoader classLoaderObject) {
        ClassContext classContext = classLoaderContexts.get(classLoaderObject);
        if (classContext == null) {
            classContext = new ClassContextImpl(this, classLoaderObject, vm::loadClass, vm::loadResource, vm::loadResources);
            final ClassContext appearing = classLoaderContexts.putIfAbsent(classLoaderObject, classContext);
            if (appearing != null) {
                classContext = appearing;
            } else {
                handleNewClassContext(classContext);
            }
        }
        return classContext;
    }

    public ClassContext constructAppClassLoaderClassContext(VmClassLoader appClassLoaderObject) {
        return appClassContextFactory.apply(appClassLoaderObject);
    }

    public ClassContext constructPlatformClassContext(VmClassLoader platformClassLoaderObject) {
        return platformClassContextFactory.apply(platformClassLoaderObject);
    }

    private ClassContext handleNewClassContext(ClassContext classContext) {
        classContextListener.accept(classContext);
        return classContext;
    }

    @Override
    public <T> void submitTask(T item, Consumer<T> itemConsumer) {
        synchronized (queue) {
            queue.add(item);
            queue.add(itemConsumer);
            queue.notify();
        }
    }

    @Override
    public NativeMethodConfigurator getNativeMethodConfigurator() {
        return nativeMethodConfigurator;
    }

    public void registerEntryPoint(final ExecutableElement method) {
        enqueue(method);
        entryPoints.add(method);
    }

    public Path getOutputDirectory() {
        return outputDir;
    }

    public Path getOutputFile(final DefinedTypeDefinition type, final String suffix) {
        Path basePath = getOutputDirectory(type);
        String fileName = basePath.getFileName().toString() + '.' + suffix;
        return basePath.resolveSibling(fileName);
    }

    static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();

    public Path getOutputDirectory(final DefinedTypeDefinition type) {
        Path base = outputDir;
        VmClassLoader classLoader = type.getContext().getClassLoader();
        final String name = classLoader == null ? "boot" : classLoader.getName();
        base = base.resolve(name);
        String internalName = type.getInternalName();
        if (type.isHidden()) {
            internalName += "~" + ENCODER.encodeToString(type.getDigest()) + "." + type.getHiddenClassIndex();
        }
        int idx = internalName.indexOf('/');
        if (idx == -1) {
            return base.resolve(internalName);
        }
        base = base.resolve(internalName.substring(0, idx));
        int start;
        for (;;) {
            start = idx + 1;
            idx = internalName.indexOf('/', start);
            if (idx == -1) {
                return base.resolve(internalName.substring(start));
            }
            base = base.resolve(internalName.substring(start, idx));
        }
    }

    public Path getOutputDirectory(final MemberElement element) {
        Path base = getOutputDirectory(element.getEnclosingType());
        if (element instanceof InitializerElement) {
            return base.resolve("class-init");
        } else if (element instanceof FieldElement) {
            return base.resolve("fields").resolve(((FieldElement) element).getName());
        } else if (element instanceof ConstructorElement) {
            return base.resolve("ctors").resolve("ctor.id" + element.getIndex());
        } else if (element instanceof MethodElement) {
            MethodElement methodElement = (MethodElement) element;
            return base.resolve("methods").resolve(methodElement.getName() + ".id" + element.getIndex());
        } else if (element instanceof FunctionElement) {
            return base.resolve("functions").resolve(((FunctionElement) element).getName());
        } else {
            throw new UnsupportedOperationException("getOutputDirectory for element " + element.getClass());
        }
    }

    public ProgramModule getOrAddProgramModule(final DefinedTypeDefinition type) {
        return programModules.computeIfAbsent(type, t -> new ProgramModule(t, typeSystem, literalFactory));
    }

    public List<ProgramModule> getAllProgramModules() {
        return List.of(programModules.values().toArray(ProgramModule[]::new));
    }

    public org.qbicc.object.Function getExactFunction(final ExecutableElement element) {
        FunctionElement established = establishedFunctions.get(element);
        if (established != null) {
            return getExactFunction(established);
        }
        // optimistic
        org.qbicc.object.Function function = getExactFunctionIfExists(element);
        if (function != null) {
            return function;
        }
        if (! mayBeEnqueued(element)) {
            throw new IllegalArgumentException("Cannot access function for un-lowered element " + element);
        }
        return exactFunctions.computeIfAbsent(element, e -> {
            ModuleSection implicit = getImplicitSection(element);
            InvokableType elementType = element.getType();
            FunctionType functionType = getFunctionTypeForElement(element);
            return implicit.addFunction(element, getExactNameForElement(element), functionType);
        });
    }

    @Override
    public org.qbicc.object.Function getExactFunctionIfExists(final ExecutableElement element) {
        FunctionElement established = establishedFunctions.get(element);
        if (established != null) {
            return getExactFunctionIfExists(established);
        }
        return exactFunctions.get(element);
    }

    @Override
    public FunctionElement establishExactFunction(ExecutableElement element, FunctionElement function) {
        FunctionElement existing = establishedFunctions.putIfAbsent(element, function);
        if (existing != null) {
            throw new IllegalStateException(
                String.format("Attempted to map a function that was already mapped: element %s cannot map to %s because it is mapped to %s already",
                    element, function, existing));
        }
        return function;
    }

    public DefinedTypeDefinition getDefaultTypeDefinition() {
        if (defaultTypeDefinition != null) {
            return defaultTypeDefinition;
        } else {
            synchronized (this) {
                if (defaultTypeDefinition != null) {
                    return defaultTypeDefinition;
                } else {
                    DefinedTypeDefinition.Builder typeBuilder = DefinedTypeDefinition.Builder.basic();
                    ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(bootstrapClassContext, "__qbicc-default-type__");
                    typeBuilder.setDescriptor(desc);
                    typeBuilder.setSignature(ClassSignature.synthesize(bootstrapClassContext, null, List.of()));
                    typeBuilder.setName("__qbicc-default-type__");
                    typeBuilder.setContext(bootstrapClassContext);
                    typeBuilder.setModifiers(ClassFile.ACC_FINAL | ClassFile.ACC_PUBLIC | ClassFile.I_ACC_NO_REFLECT);
                    typeBuilder.setInitializer((index, enclosing, builder) -> {
                        builder.setEnclosingType(enclosing);
                        return builder.build();
                    }, 0);
                    DefinedTypeDefinition defaultType = typeBuilder.build();
                    defaultTypeDefinition = defaultType;
                    return defaultType;
                }
            }
        }
    }

    public ModuleSection getImplicitSection(ExecutableElement element) {
        return getImplicitSection(element.getEnclosingType());
    }

    public ModuleSection getImplicitSection(DefinedTypeDefinition typeDefinition) {
        return getOrAddProgramModule(typeDefinition).inSection(getImplicitSection());
    }

    @Override
    public Section getImplicitSection() {
        return implicitSection;
    }

    @Override
    public Vm getVm() {
        return vm;
    }

    private static final char[] hexDigits = "0123456789abcdef".toCharArray();

    private StringBuilder appendHex(StringBuilder b, char hex) {
        b.append(hexDigits[hex >> 12 & 0xf]);
        b.append(hexDigits[hex >> 8 & 0xf]);
        b.append(hexDigits[hex >> 4 & 0xf]);
        b.append(hexDigits[hex >> 0 & 0xf]);
        return b;
    }

    private StringBuilder mangleTo(StringBuilder b, String str) {
        int length = str.length();
        for (int i = 0; i < length; i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '/' -> b.append('_');
                case '_' -> b.append("_1");
                case ';' -> b.append("_2");
                case '[' -> b.append("_3");
                // extensions
                case '<' -> b.append("_4");
                case '>' -> b.append("_5");
                default -> {
                    if ('A' <= ch && ch <= 'Z' || 'a' <= ch && ch <= 'z' || '0' <= ch && ch <= '9' || ch == '$' || ch == '.') {
                        b.append(ch);
                    } else {
                        appendHex(b.append("_0"), ch);
                    }
                }
            }
        }
        return b;
    }

    @Override
    public String getExactNameForElement(final ExecutableElement element) {
        // todo: encode class loader ID
        // todo: cache :-(
        DefinedTypeDefinition enclosingType = element.getEnclosingType();
        String internalName = enclosingType.getInternalName();
        if (enclosingType.isHidden()) {
            internalName += '$' + ENCODER.encodeToString(enclosingType.getDigest()) + '.' + enclosingType.getHiddenClassIndex();
        }
        if (element instanceof FunctionElement fe) {
            return fe.getName();
        }
        StringBuilder b = new StringBuilder(internalName.length() << 1);
        b.append("_J"); // identify Java mangled name
        mangleTo(b, internalName);
        b.append('_');
        boolean overloaded;
        if (element instanceof InitializerElement) {
            mangleTo(b, "<clinit>");
            overloaded = false;
        } else if (element instanceof ConstructorElement) {
            mangleTo(b, "<init>");
            overloaded = true; // todo: detect
        } else if (element instanceof MethodElement me) {
            mangleTo(b, me.getName());
            overloaded = true; // todo: detect
        } else {
            throw new IllegalStateException();
        }
        if (overloaded) {
            b.append("__");
            MethodDescriptor elementDescriptor = element.getDescriptor();
            for (TypeDescriptor descriptor : elementDescriptor.getParameterTypes()) {
                mangleDescriptorTo(b, enclosingType, descriptor);
            }
            // unlike JNI, we must also add the return type (but only if one is possible)
            if (element instanceof MethodElement) {
                b.append("_");
                mangleDescriptorTo(b, enclosingType, elementDescriptor.getReturnType());
            }
        }
        return b.toString();
    }

    private void mangleDescriptorTo(final StringBuilder b, final DefinedTypeDefinition enclosingType, final TypeDescriptor descriptor) {
        if (descriptor instanceof ClassTypeDescriptor ctd && ctd.packageAndClassNameEquals("org/qbicc/runtime", "CNative$ptr")) {
            // special case: pointers
            b.append('P');
        } else if (descriptor.equals(enclosingType.getDescriptor())) {
            // include the hidden class identifier
            b.append('L');
            mangleTo(b, enclosingType.getInternalName());
            mangleTo(b, ";");
        } else {
            mangleTo(b, descriptor.toString());
        }
    }

    @Override
    public FunctionType getFunctionTypeForInvokableType(final InvokableType origType) {
        int pcnt = origType.getParameterCount();
        if (origType instanceof FunctionType ft) {
            // already a function
            return ft;
        } else {
            StructType threadNativeType = (StructType) getBootstrapClassContext().resolveTypeFromClassName("jdk/internal/thread", "ThreadNative$thread_native");
            // some kind of method
            assert origType instanceof MethodType;
            MethodType mt = (MethodType) origType;
            ArrayList<ValueType> argTypes;
            argTypes = new ArrayList<>(pcnt + 2);
            argTypes.add(threadNativeType.getPointer());
            if (origType instanceof InstanceMethodType imt) {
                // instance methods also get the receiver
                argTypes.add(imt.getReceiverType());
            }
            argTypes.addAll(origType.getParameterTypes());
            return typeSystem.getFunctionType(mt.getReturnType(), List.copyOf(argTypes));
        }
    }

    public FunctionType getFunctionTypeForElement(final ExecutableElement element) {
        // look up the thread ID literal - todo: lazy cache?
        return getFunctionTypeForInvokableType(element.getType());
    }

    public FunctionType getFunctionTypeForInitializer() {
        // look up the thread arg type - todo: lazy cache?
        StructType threadNativeType = (StructType) getBootstrapClassContext().resolveTypeFromClassName("jdk/internal/thread", "ThreadNative$thread_native");
        return typeSystem.getFunctionType(typeSystem.getVoidType(), List.of(threadNativeType.getPointer()));
    }

    public Iterable<ExecutableElement> getEntryPoints() {
        return entryPoints;
    }

    BiFunction<BasicBlockBuilder.FactoryContext, ExecutableElement, BasicBlockBuilder> getBlockFactory() {
        return blockFactory;
    }

    void setBlockFactory(final BiFunction<BasicBlockBuilder.FactoryContext, ExecutableElement, BasicBlockBuilder> blockFactory) {
        this.blockFactory = blockFactory;
    }

    void setCopier(final BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> copier) {
        this.copier = copier;
    }

    private int state;
    private int activeThreads;
    private int threadAcks;
    private Consumer<CompilationContext> task;
    private volatile BiConsumer<Consumer<CompilationContext>, CompilationContext> taskRunner = Consumer::accept;

    private static final int ST_WAITING = 0; // waiting -> task | exit
    private static final int ST_TASK = 1; // task -> join
    private static final int ST_JOIN = 2; // join -> waiting
    private static final int ST_EXIT = 3; // exit -> .

    private final Runnable threadTask = new Runnable() {
        public void run() {
            CompilationContextImpl lock = CompilationContextImpl.this;
            synchronized (lock) {
                activeThreads ++;
            }
            Consumer<CompilationContext> task;
            boolean needsJoin = false;
            int state;
            for (;;) {
                synchronized (lock) {
                    inner: for (;;) {
                        state = CompilationContextImpl.this.state;
                        switch (state) {
                            case ST_WAITING: {
                                try {
                                    lock.wait();
                                } catch (InterruptedException ignored) {
                                    // consume interruption on root task
                                }
                                continue inner;
                            }
                            case ST_TASK: {
                                if (needsJoin) {
                                    try {
                                        lock.wait();
                                    } catch (InterruptedException ignored) {
                                        // consume interruption on root task
                                    }
                                    continue inner;
                                }
                                needsJoin = true;
                                // exit lock to run task
                                task = CompilationContextImpl.this.task;
                                // acknowledge
                                if (++ threadAcks == activeThreads) {
                                    lock.notifyAll();
                                }
                                break inner;
                            }
                            case ST_JOIN: {
                                needsJoin = false;
                                if (++ threadAcks == activeThreads) {
                                    lock.notifyAll();
                                }
                                try {
                                    lock.wait();
                                } catch (InterruptedException ignored) {
                                    // consume interruption on root task
                                }
                                continue inner;
                            }
                            case ST_EXIT: {
                                if (--activeThreads == 0) {
                                    lock.notifyAll();
                                }
                                return;
                            }
                            default: {
                                throw Assert.impossibleSwitchCase(state);
                            }
                        }
                    }
                }
                assert state == ST_TASK;
                try {
                    taskRunner.accept(task, lock);
                } catch (Throwable t) {
                    log.error("An exception was thrown from a parallel task", t);
                    error(t, "A task threw an uncaught exception: %s", t);
                }
            }
        }
    };

    @Override
    public void setTaskRunner(BiConsumer<Consumer<CompilationContext>, CompilationContext> taskRunner) throws IllegalStateException {
        Assert.checkNotNullParam("taskRunner", taskRunner);
        synchronized (this) {
            if (state != ST_WAITING) {
                throw new IllegalStateException("Invalid thread state");
            }
            this.taskRunner = taskRunner;
        }
    }

    @Override
    public void runWrappedTask(Consumer<CompilationContext> task) {
        taskRunner.accept(task, this);
    }

    @Override
    public void runParallelTask(Consumer<CompilationContext> task) throws IllegalStateException {
        Assert.checkNotNullParam("task", task);
        boolean intr = false;
        try {
            synchronized (this) {
                if (state != ST_WAITING) {
                    throw new IllegalStateException("Invalid thread state");
                }
                // submit task
                this.task = task;
                threadAcks = 0;
                state = ST_TASK;
                notifyAll();
                // wait for acks
                while (threadAcks < activeThreads) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
                // all threads have ack'd the task, now join
                threadAcks = 0;
                state = ST_JOIN;
                notifyAll();
                // wait for acks
                while (threadAcks < activeThreads) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
                threadAcks = 0;
                state = ST_WAITING;
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> getCopier() {
        BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock>, NodeVisitor<Node.Copier, Value, Node, BasicBlock>> copier = this.copier;
        if (copier == null) {
            throw new IllegalStateException();
        }
        return copier;
    }

    int waiting;

    void processQueue() {
        synchronized (this) {
            waiting = 0;
        }
        runParallelTask(ctxt -> {
            Object item;
            Consumer<?> consumer;
            for (;;) {
                synchronized (queue) {
                    item = queue.poll();
                    if (item == null) {
                        waiting++;
                        if (waiting == activeThreads) {
                            // no elements left! let everyone know
                            queue.notifyAll();
                            return;
                        }
                        for (;;) {
                            try {
                                queue.wait();
                            } catch (InterruptedException ignored) {
                                // safe to ignore
                            }
                            item = queue.poll();
                            if (item != null) {
                                break;
                            }
                            if (waiting == activeThreads) {
                                // awoken from sleep to exit
                                return;
                            }
                        }
                        waiting--;
                    }
                    consumer = (Consumer<?>) queue.poll();
                }
                assert consumer != null;
                try {
                    safeAccept(consumer, item);
                } catch (Throwable e) {
                    log.error("An exception was thrown from a queue processing task", e);
                    if (item instanceof Locatable loc) {
                        error(loc.getLocation(), "Exception while processing queue task %s for %s: %s", consumer, item, e);
                    } else {
                        error("Exception while processing queue task %s for %s: %s", consumer, item, e);
                    }
                }
            }
        });
    }

    static <T> void safeAccept(Consumer<T> consumer, Object item) {
        consumer.accept((T) item);
    }

    void startThreads(final int threadCnt, final long stackSize) {
        ThreadGroup threadGroup = new ThreadGroup("qbicc compiler thread group");
        Thread[] threads = new Thread[threadCnt];
        for (int i = 0; i < threadCnt; i ++) {
            threads[i] = new Thread(threadGroup, threadTask, "qbicc compiler thread " + (i + 1) + "/" + threadCnt, stackSize, false);
        }
        // now start them all
        for (int i = 0; i < threadCnt; i ++) {
            try {
                threads[i].start();
            } catch (Exception e) {
                // failed to start thread
                error("Failed to start a compiler thread: %s", e);
                exitThreads();
                return;
            }
        }
    }

    void exitThreads() {
        boolean intr = false;
        try {
            int state;
            synchronized (this) {
                state = this.state;
                if (state != ST_WAITING) {
                    throw new IllegalStateException("Unexpected thread state");
                }
                this.state = ST_EXIT;
                notifyAll();
                for (;;) {
                    if (activeThreads == 0) {
                        return;
                    }
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> getTypeBuilderFactories() {
        return typeBuilderFactories;
    }

    static Builder builder() {
        return new Builder();
    }

    MethodTypeId getMethodType(final TypeId returnTypeId, final List<TypeId> paramTypeIds, MethodDescriptor descriptor) {
        ConcurrentMap<List<TypeId>, MethodTypeId> subMap = methodTypes.get(returnTypeId);
        if (subMap == null) {
            subMap = new ConcurrentHashMap<>();
            final ConcurrentMap<List<TypeId>, MethodTypeId> appearing = methodTypes.putIfAbsent(returnTypeId, subMap);
            if (appearing != null) {
                subMap = appearing;
            }
        }
        MethodTypeId methodType = subMap.get(paramTypeIds);
        if (methodType == null) {
            methodType = new MethodTypeId(List.copyOf(paramTypeIds), returnTypeId, descriptor);
            final MethodTypeId appearing = subMap.putIfAbsent(paramTypeIds, methodType);
            if (appearing != null) {
                methodType = appearing;
            }
        }
        return methodType;
    }

    static final class Builder {
        BaseDiagnosticContext baseDiagnosticContext;
        Platform platform;
        TypeSystem typeSystem;
        LiteralFactory literalFactory;
        Scheduler scheduler;
        BiFunction<ClassContext, String, DefinedTypeDefinition> bootstrapFinder;
        BiFunction<ClassContext, String, byte[]> bootstrapResourceFinder;
        BiFunction<ClassContext, String, List<byte[]>> bootstrapResourcesFinder;
        BiFunction<ClassContext, String, DefinedTypeDefinition> appFinder;
        BiFunction<ClassContext, String, byte[]> appResourceFinder;
        BiFunction<ClassContext, String, List<byte[]>> appResourcesFinder;
        BiFunction<ClassContext, String, DefinedTypeDefinition> platformFinder;
        BiFunction<ClassContext, String, byte[]> platformResourceFinder;
        BiFunction<ClassContext, String, List<byte[]>> platformResourcesFinder;
        BiFunction<BasicBlockBuilder.FactoryContext, ExecutableElement, BasicBlockBuilder> initialBlockFactory;
        Function<CompilationContext, Vm> vmFactory;
        Path outputDir;
        List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories;
        List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories;
        NativeMethodConfigurator nativeMethodConfigurator;
        Consumer<ClassContext> classContextListener;

        private Builder() {}

        Builder setBaseDiagnosticContext(BaseDiagnosticContext baseDiagnosticContext) {
            this.baseDiagnosticContext = baseDiagnosticContext;
            return this;
        }

        Builder setPlatform(Platform platform) {
            this.platform = platform;
            return this;
        }

        Builder setTypeSystem(TypeSystem typeSystem) {
            this.typeSystem = typeSystem;
            return this;
        }

        Builder setLiteralFactory(LiteralFactory literalFactory) {
            this.literalFactory = literalFactory;
            return this;
        }

        Builder setScheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        Builder setBootstrapFinder(BiFunction<ClassContext, String, DefinedTypeDefinition> bootstrapFinder) {
            this.bootstrapFinder = bootstrapFinder;
            return this;
        }

        Builder setBootstrapResourceFinder(BiFunction<ClassContext, String, byte[]> bootstrapResourceFinder) {
            this.bootstrapResourceFinder = bootstrapResourceFinder;
            return this;
        }

        Builder setBootstrapResourcesFinder(BiFunction<ClassContext, String, List<byte[]>> bootstrapResourcesFinder) {
            this.bootstrapResourcesFinder = bootstrapResourcesFinder;
            return this;
        }

        Builder setAppFinder(BiFunction<ClassContext, String, DefinedTypeDefinition> appFinder) {
            this.appFinder = appFinder;
            return this;
        }

        Builder setAppResourceFinder(BiFunction<ClassContext, String, byte[]> appResourceFinder) {
            this.appResourceFinder = appResourceFinder;
            return this;
        }

        Builder setAppResourcesFinder(BiFunction<ClassContext, String, List<byte[]>> appResourcesFinder) {
            this.appResourcesFinder = appResourcesFinder;
            return this;
        }

        Builder setPlatformFinder(BiFunction<ClassContext, String, DefinedTypeDefinition> platformFinder) {
            this.platformFinder = platformFinder;
            return this;
        }

        Builder setPlatformResourceFinder(BiFunction<ClassContext, String, byte[]> platformResourceFinder) {
            this.platformResourceFinder = platformResourceFinder;
            return this;
        }

        Builder setPlatformResourcesFinder(BiFunction<ClassContext, String, List<byte[]>> platformResourcesFinder) {
            this.platformResourcesFinder = platformResourcesFinder;
            return this;
        }

        Builder setInitialBlockFactory(BiFunction<BasicBlockBuilder.FactoryContext, ExecutableElement, BasicBlockBuilder> blockFactory) {
            this.initialBlockFactory = blockFactory;
            return this;
        }

        Builder setVmFactory(Function<CompilationContext, Vm> vmFactory) {
            this.vmFactory = vmFactory;
            return this;
        }

        Builder setOutputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        Builder setResolverFactories(List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories) {
            this.resolverFactories = resolverFactories;
            return this;
        }

        Builder setTypeBuilderFactories(List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories) {
            this.typeBuilderFactories = typeBuilderFactories;
            return this;
        }

        Builder setNativeMethodConfigurator(NativeMethodConfigurator nativeMethodConfigurator) {
            this.nativeMethodConfigurator = nativeMethodConfigurator;
            return this;
        }

        Builder setClassContextListener(Consumer<ClassContext> classContextListener) {
            this.classContextListener = classContextListener;
            return this;
        }

        CompilationContextImpl build() {
            return new CompilationContextImpl(this);
        }
    }
}
