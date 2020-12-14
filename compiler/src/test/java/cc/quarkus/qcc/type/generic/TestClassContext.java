package cc.quarkus.qcc.type.generic;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cc.quarkus.qcc.context.AttachmentKey;
import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.context.Diagnostic;
import cc.quarkus.qcc.context.Location;
import cc.quarkus.qcc.graph.BasicBlockBuilder;
import cc.quarkus.qcc.graph.Node;
import cc.quarkus.qcc.graph.literal.ClassTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.CurrentThreadLiteral;
import cc.quarkus.qcc.graph.literal.InterfaceTypeIdLiteral;
import cc.quarkus.qcc.graph.literal.LiteralFactory;
import cc.quarkus.qcc.graph.literal.TypeIdLiteral;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.object.Function;
import cc.quarkus.qcc.object.ProgramModule;
import cc.quarkus.qcc.type.FunctionType;
import cc.quarkus.qcc.type.TypeSystem;
import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.annotation.type.TypeAnnotationList;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.Element;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MemberElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import cc.quarkus.qcc.type.descriptor.TypeDescriptor;

/**
 *
 */
public class TestClassContext implements ClassContext {
    private final CompilationContext ctxt = new CompilationContext() {
        private final Map<AttachmentKey<?>, Object> attachments = new HashMap<>();

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

        public void enqueue(final ExecutableElement element) {

        }

        public boolean wasEnqueued(final ExecutableElement element) {
            return false;
        }

        public ExecutableElement dequeue() {
            return null;
        }

        public void registerEntryPoint(final MethodElement method) {

        }

        public Path getOutputDirectory() {
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

        public Function getExactFunction(final ExecutableElement element) {
            return null;
        }

        public CurrentThreadLiteral getCurrentThreadValue() {
            return null;
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

    public DefinedTypeDefinition resolveDefinedTypeLiteral(final TypeIdLiteral typeId) {
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

    public void registerClassLiteral(final ClassTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {

    }

    public void registerInterfaceLiteral(final InterfaceTypeIdLiteral literal, final DefinedTypeDefinition typeDef) {

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

    public ValueType resolveTypeFromDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return null;
    }

    public FunctionType resolveMethodFunctionType(final MethodDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final MethodSignature signature, final TypeAnnotationList returnTypeVisible, final List<TypeAnnotationList> visibleAnnotations, final TypeAnnotationList returnTypeInvisible, final List<TypeAnnotationList> invisibleAnnotations) {
        return null;
    }

    public ValueType resolveTypeFromMethodDescriptor(final TypeDescriptor descriptor, final List<ParameterizedSignature> typeParamCtxt, final TypeSignature signature, final TypeAnnotationList visibleAnnotations, final TypeAnnotationList invisibleAnnotations) {
        return null;
    }
}
