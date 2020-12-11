package cc.quarkus.qcc.driver;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.object.Section;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.DescriptorTypeResolver;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.InvokableElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;

final class CompilationContextImpl implements CompilationContext {
    private final TypeSystem typeSystem;
    private final LiteralFactory literalFactory;
    private final BaseDiagnosticContext baseDiagnosticContext;
    private final ConcurrentMap<VmObject, ClassContext> classLoaderContexts = new ConcurrentHashMap<>();
    final Set<ExecutableElement> queued = ConcurrentHashMap.newKeySet();
    final Queue<ExecutableElement> queue = new ConcurrentLinkedDeque<>();
    final Set<MethodElement> entryPoints = ConcurrentHashMap.newKeySet();
    final ClassContext bootstrapClassContext;
    private final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory;
    private final BiFunction<VmObject, String, DefinedTypeDefinition> finder;
    private final ConcurrentMap<DefinedTypeDefinition, ProgramModule> programModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<ExecutableElement, cc.quarkus.qcc.object.Function> exactFunctions = new ConcurrentHashMap<>();
    private final ConcurrentMap<MethodElement, cc.quarkus.qcc.object.Function> virtualFunctions = new ConcurrentHashMap<>();
    private final Path outputDir;
    final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories;

    CompilationContextImpl(final BaseDiagnosticContext baseDiagnosticContext, final TypeSystem typeSystem, final LiteralFactory literalFactory, final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory, final BiFunction<VmObject, String, DefinedTypeDefinition> finder, final Path outputDir, final List<BiFunction<? super ClassContext, DescriptorTypeResolver, DescriptorTypeResolver>> resolverFactories) {
        this.baseDiagnosticContext = baseDiagnosticContext;
        this.typeSystem = typeSystem;
        this.literalFactory = literalFactory;
        this.blockFactory = blockFactory;
        this.finder = finder;
        this.outputDir = outputDir;
        this.resolverFactories = resolverFactories;
        bootstrapClassContext = new ClassContextImpl(this, null);
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

    public ClassContext constructClassContext(final VmObject classLoaderObject) {
        return classLoaderContexts.computeIfAbsent(classLoaderObject, classLoader -> new ClassContextImpl(this, classLoader));
    }

    public void enqueue(final ExecutableElement element) {
        if (queued.add(element)) {
            queue.add(element);
        }
    }

    public boolean wasEnqueued(final ExecutableElement element) {
        return queued.contains(element);
    }

    public ExecutableElement dequeue() {
        return queue.poll();
    }

    void clearEnqueuedSet() {
        queued.clear();
    }

    public void registerEntryPoint(final MethodElement method) {
        entryPoints.add(method);
    }

    public Path getOutputDirectory() {
        return outputDir;
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

    public cc.quarkus.qcc.object.Function getExactFunction(final ExecutableElement element) {
        // optimistic
        cc.quarkus.qcc.object.Function function = exactFunctions.get(element);
        if (function != null) {
            return function;
        }
        // look up the thread ID literal - todo: lazy cache?
        TypeIdLiteral threadTypeId = bootstrapClassContext.findDefinedType("java/lang/Thread").validate().getTypeId();
        ProgramModule programModule = getOrAddProgramModule(element.getEnclosingType());
        Section implicit = programModule.getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        return exactFunctions.computeIfAbsent(element, e -> {
            FunctionType type = getFunctionTypeForElement(typeSystem, element, threadTypeId);
            return implicit.addFunction(element, getExactNameForElement(element, type), type);
        });
    }

    public cc.quarkus.qcc.object.Function getVirtualFunction(final MethodElement element) {
        // optimistic
        cc.quarkus.qcc.object.Function function = virtualFunctions.get(element);
        if (function != null) {
            return function;
        }
        // look up the thread ID literal - todo: lazy cache?
        TypeIdLiteral threadTypeId = bootstrapClassContext.findDefinedType("java/lang/Thread").validate().getTypeId();
        ProgramModule programModule = getOrAddProgramModule(element.getEnclosingType());
        Section implicit = programModule.getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        return exactFunctions.computeIfAbsent(element, e -> {
            FunctionType type = getFunctionTypeForElement(typeSystem, element, threadTypeId);
            return implicit.addFunction(element, getVirtualNameForElement(element, type), type);
        });
    }

    public CurrentThreadLiteral getCurrentThreadValue() {
        // look up the thread ID literal - todo: lazy cache?
        TypeIdLiteral threadTypeId = bootstrapClassContext.findDefinedType("java/lang/Thread").validate().getTypeId();
        // construct the literal - todo: cache
        return literalFactory.literalOfCurrentThread(typeSystem.getReferenceType(threadTypeId));
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
        b.append(parameterCount);
        for (int i = 0; i < parameterCount; i ++) {
            b.append('.');
            type.getParameterType(i).toFriendlyString(b);
        }
        return b.toString();
    }

    private FunctionType getFunctionTypeForElement(TypeSystem ts, ExecutableElement element, final TypeIdLiteral threadTypeId) {
        if (element instanceof InitializerElement) {
            // todo: initializers should not survive the copy
            return ts.getFunctionType(ts.getVoidType());
        }
        assert element instanceof InvokableElement;
        FunctionType methodType = element.getType(element.getEnclosingType().getContext(), List.of(/*todo*/));
        // function type is the same as the method type, except with current thread/receiver first
        int pcnt = methodType.getParameterCount();
        ValueType[] argTypes;
        int j;
        if (element.isStatic()) {
            argTypes = new ValueType[pcnt + 1];
            j = 1;
        } else {
            argTypes = new ValueType[pcnt + 2];
            argTypes[1] = ts.getReferenceType(element.getEnclosingType().validate().getTypeId());
            j = 2;
        }
        argTypes[0] = ts.getReferenceType(threadTypeId);
        for (int i = 0; i < pcnt; i ++, j ++) {
            argTypes[j] = methodType.getParameterType(i);
        }
        return ts.getFunctionType(methodType.getReturnType(), argTypes);
    }

    public Iterable<MethodElement> getEntryPoints() {
        return entryPoints;
    }

    BiFunction<VmObject, String, DefinedTypeDefinition> getFinder() {
        return finder;
    }

    BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> getBlockFactory() {
        return blockFactory;
    }
}
