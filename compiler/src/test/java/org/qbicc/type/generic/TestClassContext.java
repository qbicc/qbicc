package org.qbicc.type.generic;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.context.Diagnostic;
import org.qbicc.context.Location;
import org.qbicc.graph.BasicBlockBuilder;
import org.qbicc.graph.Node;
import org.qbicc.graph.literal.LiteralFactory;
import org.qbicc.graph.literal.SymbolLiteral;
import org.qbicc.interpreter.VmObject;
import org.qbicc.machine.arch.Platform;
import org.qbicc.object.Function;
import org.qbicc.object.FunctionDeclaration;
import org.qbicc.object.ProgramModule;
import org.qbicc.object.Section;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.FunctionType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.ValueType;
import org.qbicc.type.annotation.type.TypeAnnotationList;
import org.qbicc.context.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.Element;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.MemberElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class TestClassContext implements ClassContext {
    private final CompilationContext ctxt = new CompilationContext() {
        private final Map<AttachmentKey<?>, Object> attachments = new HashMap<>();

        public Platform getPlatform() {
            return Platform.HOST_PLATFORM;
        }

        public TypeSystem getTypeSystem() {
            return null;
        }

        public LiteralFactory getLiteralFactory() {
            return null;
        }

        public ClassContext getBootstrapClassContext() {
            return null;
        }

        public ClassContext constructClassContext(final VmObject classLoaderObject) {
            return null;
        }

        public MethodElement getVMHelperMethod(String helperName) { return null; }

        public void enqueue(final ExecutableElement element) {

        }

        public boolean wasEnqueued(final ExecutableElement element) {
            return false;
        }

        public ExecutableElement dequeue() {
            return null;
        }

        public void registerEntryPoint(final ExecutableElement method) {

        }

        public Path getOutputDirectory() {
            return null;
        }

        public Path getOutputFile(final DefinedTypeDefinition type, final String suffix) {
            return null;
        }

        public Path getOutputDirectory(final DefinedTypeDefinition type) {
            return null;
        }

        public Path getOutputDirectory(final MemberElement element) {
            return null;
        }

        public ProgramModule getOrAddProgramModule(final DefinedTypeDefinition type) {
            return null;
        }

        public List<ProgramModule> getAllProgramModules() {
            return null;
        }

        public Section getImplicitSection(ExecutableElement element) {
            return null;  // TODO: Customise this generated block
        }
        
        public Section getImplicitSection(DefinedTypeDefinition typeDefinition) {
            return null;  // TODO: Customise this generated block
        }

        public Function getExactFunction(final ExecutableElement element) {
            return null;
        }

        public Function getExactFunctionIfExists(ExecutableElement element) {
            return null;
        }

        public FunctionType getFunctionTypeForElement(ExecutableElement element) {
            return null;
        }

        public FunctionDeclaration declareForeignFunction(ExecutableElement target, Function function, ExecutableElement current) {
            return null;  // TODO: Customise this generated block
        }

        public SymbolLiteral getCurrentThreadLocalSymbolLiteral() {
            return null;
        }

        public FieldElement getExceptionField() {
            return null;
        }

        @Override
        public List<String> findBootstrapConfigurationFiles(String fileName) {
            return null;
        }

        public void runParallelTask(Consumer<CompilationContext> task) throws IllegalStateException {
        }

        public <T> T getAttachment(final AttachmentKey<T> key) {
            return (T) attachments.get(key);
        }

        public <T> T getAttachmentOrDefault(final AttachmentKey<T> key, final T defVal) {
            return (T) attachments.getOrDefault(key, defVal);
        }

        public <T> T putAttachment(final AttachmentKey<T> key, final T value) {
            return (T) attachments.put(key, value);
        }

        public <T> T putAttachmentIfAbsent(final AttachmentKey<T> key, final T value) {
            return (T) attachments.putIfAbsent(key, value);
        }

        public <T> T removeAttachment(final AttachmentKey<T> key) {
            return (T) attachments.remove(key);
        }

        public <T> boolean removeAttachment(final AttachmentKey<T> key, final T expect) {
            return attachments.remove(key, expect);
        }

        public <T> T replaceAttachment(final AttachmentKey<T> key, final T update) {
            return (T) attachments.replace(key, update);
        }

        public <T> boolean replaceAttachment(final AttachmentKey<T> key, final T expect, final T update) {
            return attachments.replace(key, expect, update);
        }

        public <T> T computeAttachmentIfAbsent(final AttachmentKey<T> key, final Supplier<T> function) {
            return (T) attachments.computeIfAbsent(key, k -> function.get());
        }

        public <T> T computeAttachmentIfPresent(final AttachmentKey<T> key, final java.util.function.Function<T, T> function) {
            return (T) attachments.computeIfPresent(key, (k, o) -> function.apply((T) o));
        }

        public <T> T computeAttachment(final AttachmentKey<T> key, final java.util.function.Function<T, T> function) {
            return (T) attachments.compute(key, (k, o) -> function.apply((T) o));
        }

        public int errors() {
            return 0;
        }

        public int warnings() {
            return 0;
        }

        public Diagnostic msg(final Diagnostic parent, final Location location, final Diagnostic.Level level, final String fmt, final Object... args) {
            return null;
        }

        public Diagnostic msg(final Diagnostic parent, final Element element, final Node node, final Diagnostic.Level level, final String fmt, final Object... args) {
            return null;
        }

        public Iterable<Diagnostic> getDiagnostics() {
            return null;
        }
    };

    public CompilationContext getCompilationContext() {
        return ctxt;
    }

    public VmObject getClassLoader() {
        return null;
    }

    public DefinedTypeDefinition findDefinedType(final String typeName) {
        return null;
    }

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final ObjectType typeId) {
        return null;
    }

    public String deduplicate(final ByteBuffer buffer, final int offset, final int length) {
        byte[] b = new byte[length];
        int old = buffer.position();
        buffer.position(offset);
        buffer.get(b);
        buffer.position(old);
        return deduplicate(new String(b, StandardCharsets.UTF_8));
    }

    public String deduplicate(final String original) {
        return original.intern();
    }

    public TypeSystem getTypeSystem() {
        return null;
    }

    public LiteralFactory getLiteralFactory() {
        return null;
    }

    public BasicBlockBuilder newBasicBlockBuilder(final ExecutableElement element) {
        return null;
    }

    public void defineClass(final String name, final DefinedTypeDefinition definition) {

    }

    public ValueType resolveTypeFromClassName(final String packageName, final String internalName) {
        return null;
    }

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return null;
    }

    public ArrayObjectType resolveArrayObjectTypeFromDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visible, final TypeAnnotationList invisible) {
        return null;
    }

    public FunctionType resolveMethodFunctionType(final MethodDescriptor descriptor, TypeParameterContext paramCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations) {
        return null;
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, TypeParameterContext paramCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return null;
    }
}
