package org.qbicc.driver;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.common.constraint.Assert;
import org.jboss.logging.Logger;
import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.Location;
import org.qbicc.graph.BasicBlock;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.NodeVisitor;
import org.qbicc.graph.Value;
import org.qbicc.graph.ValueHandle;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.DescriptorTypeResolver;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.FunctionElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.InvokableElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.generic.ClassSignature;
import org.qbicc.type.generic.TypeSignature;

final class CompilationContextImpl implements CompilationContext {
    private static final Logger log = Logger.getLogger("org.qbicc.driver");

    private final Platform platform;
    private final TypeSystem typeSystem;
    private final LiteralFactory literalFactory;
    private final BaseDiagnosticContext baseDiagnosticContext;
    private final ConcurrentMap<VmClassLoader, ClassContext> classLoaderContexts = new ConcurrentHashMap<>();
    volatile Set<ExecutableElement> allowedSet = null;
    final Set<ExecutableElement> queued = ConcurrentHashMap.newKeySet();
    final Queue<ExecutableElement> queue = new ArrayDeque<>();
    final Set<ExecutableElement> entryPoints = ConcurrentHashMap.newKeySet();
    final ClassContext bootstrapClassContext;
    private final ConcurrentMap<DefinedTypeDefinition, ProgramModule> programModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<ExecutableElement, org.qbicc.object.Function> exactFunctions = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodElement, org.qbicc.object.Function> virtualFunctions = new ConcurrentHashMap<>();
    private final Path outputDir;
    final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories;
    private final AtomicReference<FieldElement> exceptionFieldHolder = new AtomicReference<>();
    private final SymbolLiteral qbiccBoundThread;
    private volatile DefinedTypeDefinition defaultTypeDefinition;
    private final List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories;

    // mutable state
    private volatile BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory;
    private volatile BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> copier;
    private final Vm vm;

    CompilationContextImpl(final BaseDiagnosticContext baseDiagnosticContext, Platform platform, final TypeSystem typeSystem, final LiteralFactory literalFactory, BiFunction<ClassContext, String, DefinedTypeDefinition> bootstrapFinder, Function<CompilationContext, Vm> vmFactory, final Path outputDir, final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories, List<BiFunction<? super ClassContext, DefinedTypeDefinition.Builder, DefinedTypeDefinition.Builder>> typeBuilderFactories) {
        this.baseDiagnosticContext = baseDiagnosticContext;
        this.platform = platform;
        this.typeSystem = typeSystem;
        this.literalFactory = literalFactory;
        this.outputDir = outputDir;
        this.resolverFactories = resolverFactories;
        bootstrapClassContext = new ClassContextImpl(this, null, bootstrapFinder);
        this.typeBuilderFactories = typeBuilderFactories;
        qbiccBoundThread = getLiteralFactory().literalOfSymbol("_qbicc_bound_thread", getTypeSystem().getVoidType().getPointer().asCollected().getPointer());
        // last!
        this.vm = vmFactory.apply(this);
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

    public LiteralFactory getLiteralFactory() {
        return literalFactory;
    }

    public ClassContext getBootstrapClassContext() {
        return bootstrapClassContext;
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        return baseDiagnosticContext.deduplicate(buffer, offset, length);
    }

    public String deduplicate(final String original) {
        return baseDiagnosticContext.deduplicate(original);
    }

    public ClassContext constructClassContext(final VmClassLoader classLoaderObject) {
        return classLoaderContexts.computeIfAbsent(classLoaderObject, classLoader -> new ClassContextImpl(this, classLoader, vm::loadClass));
    }

    public MethodElement getVMHelperMethod(String name) {
        DefinedTypeDefinition dtd = bootstrapClassContext.findDefinedType("org/qbicc/runtime/main/VMHelpers");
        if (dtd == null) {
            error("Can't find runtime library class: " + "org/qbicc/runtime/main/VMHelpers");
            return null;
        }
        LoadedTypeDefinition helpers = dtd.load();
        int idx = helpers.findMethodIndex(e -> name.equals(e.getName()));
        if (idx == -1) {
            error("Can't find the runtime helper method %s", name);
            return null;
        }
        return helpers.getMethod(idx);
    }

    public MethodElement getOMHelperMethod(String name) {
        DefinedTypeDefinition dtd = bootstrapClassContext.findDefinedType("org/qbicc/runtime/main/ObjectModel");
        if (dtd == null) {
            error("Can't find runtime library class: " + "org/qbicc/runtime/main/ObjectModel");
        }
        LoadedTypeDefinition helpers = dtd.load();
        int idx = helpers.findMethodIndex(e -> name.equals(e.getName()));
        if (idx == -1) {
            error("Can't find the runtime helper method %s", name);
        }
        return helpers.getMethod(idx);
    }

    public void enqueue(final ExecutableElement element) {
        Set<ExecutableElement> allowedSet = this.allowedSet;
        if (allowedSet != null && ! allowedSet.contains(element)) {
            throw new IllegalStateException("Cannot reach previously unreachable element: " + element);
        }
        if (queued.add(element)) {
            synchronized (queue) {
                queue.add(element);
                queue.notify();
            }
        }
    }

    public boolean wasEnqueued(final ExecutableElement element) {
        return queued.contains(element);
    }

    public ExecutableElement dequeue() {
        synchronized (queue) {
            return queue.poll();
        }
    }

    void lockEnqueuedSet() {
        allowedSet = Set.copyOf(queued);
    }

    void clearEnqueuedSet() {
        queued.clear();
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

    public Path getOutputDirectory(final DefinedTypeDefinition type) {
        Path base = outputDir;
        String internalName = type.getInternalName();
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

    public ProgramModule getProgramModule(final DefinedTypeDefinition type) {
        return programModules.get(type);
    }

    public ProgramModule getOrAddProgramModule(final DefinedTypeDefinition type) {
        return programModules.computeIfAbsent(type, t -> new ProgramModule(t, typeSystem, literalFactory));
    }

    public List<ProgramModule> getAllProgramModules() {
        return List.of(programModules.values().toArray(ProgramModule[]::new));
    }

    public org.qbicc.object.Function getExactFunction(final ExecutableElement element) {
        // optimistic
        org.qbicc.object.Function function = getExactFunctionIfExists(element);
        if (function != null) {
            return function;
        }
        if (! queued.contains(element)) {
            throw new IllegalArgumentException("Cannot access function for un-lowered element " + element);
        }
        return exactFunctions.computeIfAbsent(element, e -> {
            Section implicit = getImplicitSection(element);
            FunctionType elementType = element.getType();
            if (element instanceof FunctionElement) {
                return implicit.addFunction(element, ((FunctionElement) element).getName(), elementType);
            }
            FunctionType functionType = getFunctionTypeForElement(element);
            return implicit.addFunction(element, getExactNameForElement(element, elementType), functionType);
        });
    }

    @Override
    public org.qbicc.object.Function getExactFunctionIfExists(final ExecutableElement element) {
        if (element.hasAllModifiersOf(ClassFile.ACC_NATIVE)) {
            throw new IllegalArgumentException("Cannot get function of native method");
        }
        return exactFunctions.get(element);
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
                    typeBuilder.setInitializer((index, enclosing) -> {
                        InitializerElement.Builder builder = InitializerElement.builder();
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

    public Section getImplicitSection(ExecutableElement element) {
        return getImplicitSection(element.getEnclosingType());
    }

    public Section getImplicitSection(DefinedTypeDefinition typeDefinition) {
        ProgramModule programModule = getOrAddProgramModule(typeDefinition);
        return programModule.getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
    }

    public FunctionDeclaration declareForeignFunction(final ExecutableElement target, final org.qbicc.object.Function function, final ExecutableElement current) {
        if (target.getEnclosingType().equals(current.getEnclosingType())) {
            return null;
        }

        return getImplicitSection(current)
            .declareFunction(target, function.getName(), function.getType());
    }

    public org.qbicc.object.Function getVirtualFunction(final MethodElement element) {
        // optimistic
        org.qbicc.object.Function function = virtualFunctions.get(element);
        if (function != null) {
            return function;
        }
        // look up the thread ID literal - todo: lazy cache?
        ClassObjectType threadType = bootstrapClassContext.findDefinedType("java/lang/Thread").load().getClassType();
        Section implicit = getImplicitSection(element);
        return virtualFunctions.computeIfAbsent(element, e -> {
            FunctionType type = getFunctionTypeForElement(element, threadType);
            return implicit.addFunction(element, getVirtualNameForElement(element, type), type);
        });
    }

    public SymbolLiteral getCurrentThreadLocalSymbolLiteral() {
        return qbiccBoundThread;
    }

    public FieldElement getExceptionField() {
        AtomicReference<FieldElement> exceptionFieldHolder = this.exceptionFieldHolder;
        FieldElement fieldElement = exceptionFieldHolder.get();
        if (fieldElement == null) {
            synchronized (exceptionFieldHolder) {
                fieldElement = exceptionFieldHolder.get();
                if (fieldElement == null) {
                    FieldElement.Builder builder = FieldElement.builder();
                    builder.setName("thrown");
                    ClassContext classContext = this.bootstrapClassContext;
                    DefinedTypeDefinition jlt = classContext.findDefinedType("java/lang/Thread");
                    ClassTypeDescriptor desc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Throwable");
                    builder.setDescriptor(desc);
                    builder.setSignature(TypeSignature.synthesize(classContext, desc));
                    builder.setModifiers(ClassFile.ACC_PRIVATE | ClassFile.I_ACC_NO_REFLECT | ClassFile.I_ACC_NO_RESOLVE);
                    builder.setEnclosingType(jlt);
                    fieldElement = builder.build();
                    jlt.load().injectField(fieldElement);
                    exceptionFieldHolder.set(fieldElement);
                }
            }
        }
        return fieldElement;
    }

    @Override
    public Vm getVm() {
        return vm;
    }

    private String getExactNameForElement(final ExecutableElement element, final FunctionType type) {
        // todo: encode class loader ID
        // todo: cache :-(
        String internalDotName = element.getEnclosingType().getInternalName().replace('/', '.');
        if (element instanceof InitializerElement) {
            return "clinit." + internalDotName;
        }
        StringBuilder b = new StringBuilder();
        assert element instanceof InvokableElement;
        int parameterCount = type.getParameterCount();
        if (element instanceof ConstructorElement) {
            b.append("init.");
            b.append(internalDotName).append('.');
        } else {
            b.append("exact.");
            b.append(internalDotName).append('.');
            b.append(((MethodElement)element).getName()).append('.');
            type.getReturnType().toFriendlyString(b).append('.');
        }
        b.append(parameterCount);
        for (int i = 0; i < parameterCount; i ++) {
            b.append('.');
            type.getParameterType(i).toFriendlyString(b);
        }
        return b.toString();
    }

    private String getVirtualNameForElement(final MethodElement element, final FunctionType type) {
        // todo: encode class loader ID
        String internalDotName = element.getEnclosingType().getInternalName().replace('/', '.');
        StringBuilder b = new StringBuilder();
        int parameterCount = type.getParameterCount();
        b.append("virtual.");
        b.append(internalDotName).append('.');
        b.append(element.getName()).append('.');
        type.getReturnType().toFriendlyString(b).append('.');
        b.append(parameterCount - 1);
        for (int i = 1; i < parameterCount; i ++) {
            b.append('.');
            type.getParameterType(i).toFriendlyString(b);
        }
        return b.toString();
    }

    private FunctionType getFunctionTypeForElement(ExecutableElement element, final ClassObjectType threadType) {
        TypeSystem ts = typeSystem;
        if (element instanceof InitializerElement) {
            // todo: initializers should not survive the copy
            return ts.getFunctionType(ts.getVoidType(), threadType.getReference());
        }
        assert element instanceof InvokableElement;
        FunctionType methodType = element.getType();
        // function type is the same as the method type, except with current thread/receiver first
        int pcnt = methodType.getParameterCount();
        ValueType[] argTypes;
        int j;
        if (element.isStatic()) {
            argTypes = new ValueType[pcnt + 1];
            j = 1;
        } else {
            argTypes = new ValueType[pcnt + 2];
            argTypes[1] = element.getEnclosingType().load().getType().getReference();
            j = 2;
        }
        argTypes[0] = threadType.getReference();
        for (int i = 0; i < pcnt; i ++, j ++) {
            argTypes[j] = methodType.getParameterType(i);
        }
        return ts.getFunctionType(methodType.getReturnType(), argTypes);
    }

    public FunctionType getFunctionTypeForElement(final ExecutableElement element) {
        // look up the thread ID literal - todo: lazy cache?
        ClassObjectType threadType = bootstrapClassContext.findDefinedType("java/lang/Thread").load().getClassType();
        return getFunctionTypeForElement(element, threadType);
    }

    public Iterable<ExecutableElement> getEntryPoints() {
        return entryPoints;
    }

    BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> getBlockFactory() {
        return blockFactory;
    }

    void setBlockFactory(final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory) {
        this.blockFactory = blockFactory;
    }

    void setCopier(final BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> copier) {
        this.copier = copier;
    }

    private int state;
    private int activeThreads;
    private int threadAcks;
    private Consumer<CompilationContext> task;

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
                    task.accept(lock);
                } catch (Throwable t) {
                    log.error("An exception was thrown from a parallel task", t);
                    error(t, "A task threw an uncaught exception");
                }
            }
        }
    };

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
    public BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> getCopier() {
        BiFunction<CompilationContext, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>, NodeVisitor<Node.Copier, Value, Node, BasicBlock, ValueHandle>> copier = this.copier;
        if (copier == null) {
            throw new IllegalStateException();
        }
        return copier;
    }

    int waiting;

    void processQueue(Consumer<ExecutableElement> consumer) {
        synchronized (this) {
            waiting = 0;
        }
        runParallelTask(ctxt -> {
            ExecutableElement element;
            for (;;) {
                synchronized (queue) {
                    element = queue.poll();
                    if (element == null) {
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
                            element = queue.poll();
                            if (element != null) {
                                break;
                            }
                            if (waiting == activeThreads) {
                                // awoken from sleep to exit
                                return;
                            }
                        }
                        waiting--;
                    }
                }
                try {
                    consumer.accept(element);
                } catch (Throwable e) {
                    log.error("An exception was thrown from a queue processing task", e);
                    error(element, "Exception while processing queue task for element: %s", e);
                }
            }
        });
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
}
