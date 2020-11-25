package cc.quarkus.qcc.driver;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
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
import cc.quarkus.qcc.type.definition.element.BasicElement;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.FieldElement;
import cc.quarkus.qcc.type.definition.element.InitializerElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.definition.element.ParameterizedExecutableElement;

final class CompilationContextImpl implements CompilationContext {
    private final TypeSystem typeSystem;
    private final LiteralFactory literalFactory;
    private final BaseDiagnosticContext baseDiagnosticContext;
    private final ConcurrentMap<VmObject, ClassContext> classLoaderContexts = new ConcurrentHashMap<>();
    final Set<ExecutableElement> queued = ConcurrentHashMap.newKeySet();
    final Queue<ExecutableElement> queue = new ConcurrentLinkedDeque<>();
    final Set<MethodElement> entryPoints = ConcurrentHashMap.newKeySet();
    final ClassContext bootstrapClassContext = new ClassContextImpl(this, null);
    private final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory;
    private final BiFunction<VmObject, String, DefinedTypeDefinition> finder;
    private final ConcurrentMap<DefinedTypeDefinition, ProgramModule> programModules = new ConcurrentHashMap<>();
    private final ConcurrentMap<ExecutableElement, cc.quarkus.qcc.object.Function> methodToFunction = new ConcurrentHashMap<>();
    private final Path outputDir;

    CompilationContextImpl(final BaseDiagnosticContext baseDiagnosticContext, final TypeSystem typeSystem, final LiteralFactory literalFactory, final BiFunction<CompilationContext, ExecutableElement, BasicBlockBuilder> blockFactory, final BiFunction<VmObject, String, DefinedTypeDefinition> finder, final Path outputDir) {
        this.baseDiagnosticContext = baseDiagnosticContext;
        this.typeSystem = typeSystem;
        this.literalFactory = literalFactory;
        this.blockFactory = blockFactory;
        this.finder = finder;
        this.outputDir = outputDir;
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

    public Diagnostic msg(final Diagnostic parent, final BasicElement element, final Node node, final Diagnostic.Level level, final String fmt, final Object... args) {
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

    public Path getOutputDirectory(final BasicElement element) {
        Path base = getOutputDirectory(element.getEnclosingType());
        if (element instanceof InitializerElement) {
            return base.resolve("class-init");
        } else if (element instanceof FieldElement) {
            return base.resolve("fields").resolve(((FieldElement) element).getName());
        } else if (element instanceof ConstructorElement) {
            return base.resolve("ctors").resolve(signatureString((ConstructorElement) element));
        } else if (element instanceof MethodElement) {
            MethodElement methodElement = (MethodElement) element;
            return base.resolve("methods").resolve(methodElement.getName()).resolve(signatureString(methodElement));
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
        cc.quarkus.qcc.object.Function function = methodToFunction.get(element);
        if (function != null) {
            return function;
        }
        // look up the thread ID literal - todo: lazy cache?
        TypeIdLiteral threadTypeId = bootstrapClassContext.findDefinedType("java/lang/Thread").validate().getTypeId();
        ProgramModule programModule = getOrAddProgramModule(element.getEnclosingType());
        Section implicit = programModule.getOrAddSection(CompilationContext.IMPLICIT_SECTION_NAME);
        return methodToFunction.computeIfAbsent(element, e -> implicit.addFunction(element, getNameForElement(element), getFunctionTypeForElement(typeSystem, element, threadTypeId)));
    }

    private String getNameForElement(final ExecutableElement element) {
        // todo: encode class loader ID
        String internalDotName = element.getEnclosingType().getInternalName().replace('/', '.');
        if (element instanceof InitializerElement) {
            return "clinit." + internalDotName;
        }
        StringBuilder b = new StringBuilder();
        assert element instanceof ParameterizedExecutableElement;
        ParameterizedExecutableElement elementWithParam = (ParameterizedExecutableElement) element;
        int parameterCount = elementWithParam.getParameterCount();
        if (element instanceof ConstructorElement) {
            b.append("init.");
        } else {
            b.append("exact.").append(((MethodElement)element).getName());
        }
        b.append(internalDotName).append('.').append(parameterCount);
        for (int i = 0; i < parameterCount; i ++) {
            b.append('.');
            elementWithParam.getParameter(i).getType().toFriendlyString(b);
        }
        return b.toString();
    }

    private FunctionType getFunctionTypeForElement(TypeSystem ts, ExecutableElement element, final TypeIdLiteral threadTypeId) {
        if (element instanceof InitializerElement) {
            // todo: initializers should not survive the copy
            return ts.getFunctionType(ts.getVoidType());
        }
        assert element instanceof ParameterizedExecutableElement;
        ParameterizedExecutableElement elementWithParams = (ParameterizedExecutableElement) element;
        ValueType returnType;
        if (elementWithParams instanceof ConstructorElement) {
            returnType = ts.getVoidType();
        } else {
            returnType = ((MethodElement) element).getReturnType();
        }
        int parameterCount = elementWithParams.getParameterCount();
        int offset = elementWithParams.isStatic() ? 1 : 2;
        ValueType[] paramTypes = new ValueType[offset + parameterCount];
        paramTypes[0] = ts.getReferenceType(threadTypeId);
        if (offset == 2) {
            // this
            paramTypes[1] = ts.getReferenceType(element.getEnclosingType().validate().getTypeId());
        }
        for (int i = 0; i < parameterCount; i ++) {
            paramTypes[i + offset] = elementWithParams.getParameter(i).getType();
        }
        return ts.getFunctionType(returnType, paramTypes);
    }

    String signatureString(ParameterizedExecutableElement element) {
        StringBuilder builder = new StringBuilder();
        element.forEachParameter((b, e) -> e.getType().toString(b), builder);
        // this is not ideal but we're creating invalid file names all over the place otherwise
        return Base64.getUrlEncoder().encodeToString(builder.toString().getBytes(StandardCharsets.UTF_8));
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
