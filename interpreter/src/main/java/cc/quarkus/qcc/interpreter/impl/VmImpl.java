package cc.quarkus.qcc.interpreter.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.interpreter.Signal;
import cc.quarkus.qcc.interpreter.Thrown;
import cc.quarkus.qcc.interpreter.Vm;
import cc.quarkus.qcc.interpreter.VmArray;
import cc.quarkus.qcc.interpreter.VmClass;
import cc.quarkus.qcc.interpreter.VmObject;
import cc.quarkus.qcc.interpreter.VmThread;
import cc.quarkus.qcc.type.ArrayObjectType;
import cc.quarkus.qcc.type.ClassObjectType;
import cc.quarkus.qcc.type.definition.ClassContext;
import cc.quarkus.qcc.type.definition.DefinedTypeDefinition;
import cc.quarkus.qcc.type.definition.element.ConstructorElement;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.MethodElement;
import cc.quarkus.qcc.type.descriptor.MethodDescriptor;
import io.smallrye.common.constraint.Assert;

public final class VmImpl implements Vm {
    private final CompilationContext ctxt;
    private final Map<VmObject, ClassContext> classLoaders = new ConcurrentHashMap<>();
    private final MethodElement loadClass;

    VmImpl(final CompilationContext ctxt) {
        this.ctxt = ctxt;
        ClassContext bcc = ctxt.getBootstrapClassContext();
        loadClass = bcc.findDefinedType("java/lang/ClassLoader")
            .validate()
            .resolveMethodElementExact("loadClass",
                MethodDescriptor.parse(bcc, ByteBuffer.wrap("(Ljava/lang/String;)Ljava/lang/Class;".getBytes(StandardCharsets.UTF_8)))
            );
    }

    public CompilationContext getCompilationContext() {
        return ctxt;
    }

    public VmThread newThread(final String threadName, final VmObject threadGroup, final boolean daemon) {
        return null;
    }

    public DefinedTypeDefinition loadClass(final VmObject classLoader, final String name) throws Thrown {
        DefinedTypeDefinition defined = findLoadedClass(classLoader, name);
        if (defined != null) {
            return defined;
        }
        VmClass clazz = (VmClass) invokeVirtual(loadClass, classLoader, getSharedString(name));
        if (clazz == null) {
            return null;
        }
        return clazz.getTypeDefinition();
    }

    public DefinedTypeDefinition findLoadedClass(final VmObject classLoader, final String name) {
        ClassContext classContext;
        if (classLoader == null) {
            classContext = ctxt.getBootstrapClassContext();
        } else {
            classContext = classLoaders.computeIfAbsent(classLoader, ctxt::constructClassContext);
        }
        return classContext.findDefinedType(name);
    }

    public VmObject allocateObject(final ClassObjectType type) {
        return null;
    }

    public VmArray allocateArray(final ArrayObjectType type, final int length) {
        return null;
    }

    public void invokeExact(final ConstructorElement method, final VmObject instance, final Object... args) {
        getInstanceInvoker(method).invokeVoid(instance, args);
    }

    public Object invokeExact(final MethodElement method, final VmObject instance, final Object... args) {
        return null;
    }

    public Object invokeVirtual(final MethodElement method, final VmObject instance, final Object... args) {
        return null;
    }

    public void initialize(final VmClass vmClass) {
        VmThread vmThread = Vm.requireCurrentThread();

    }

    public void deliverSignal(final Signal signal) {

    }

    public VmObject getSharedString(final String string) {
        return null;
    }

    public VmObject allocateDirectBuffer(final ByteBuffer backingBuffer) {
        return null;
    }

    public DefinedTypeDefinition.Builder newTypeDefinitionBuilder(final VmObject classLoader) {
        return null;
    }

    public VmObject getMainThreadGroup() {
        return null;
    }

    public static VmImpl create(CompilationContext ctxt) {
        return new VmImpl(Assert.checkNotNullParam("ctxt", ctxt));
    }

    private InstanceInvoker getInstanceInvoker(ExecutableElement element) {
        throw new UnsupportedOperationException();
    }
}
