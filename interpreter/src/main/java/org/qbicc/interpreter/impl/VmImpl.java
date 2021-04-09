package org.qbicc.interpreter.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Signal;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.ArrayObjectType;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.definition.ClassContext;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.MethodElement;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
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
                MethodDescriptor.synthesize(bcc, ClassTypeDescriptor.synthesize(bcc, "java/lang/String"),
                                            List.of(ClassTypeDescriptor.synthesize(bcc, "java/lang/Class"))));
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
