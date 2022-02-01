package org.qbicc.interpreter.impl;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.smallrye.common.constraint.Assert;
import org.qbicc.context.ClassContext;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmArray;
import org.qbicc.interpreter.VmClassLoader;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.MethodHandleConstant;

final class VmClassLoaderImpl extends VmObjectImpl implements VmClassLoader {
    private final ClassContext classContext;
    private final Map<String, VmClassImpl> defined = new ConcurrentHashMap<>();
    final ConcurrentHashMap<MethodDescriptor, VmObject> methodTypeCache = new ConcurrentHashMap<>();
    final ConcurrentHashMap<MethodHandleConstant, VmObject> methodHandleCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicInteger> hiddenClassSeqMap = new ConcurrentHashMap<>();

    VmClassLoaderImpl(VmClassLoaderClassImpl clazz, VmImpl vm) {
        // bootstrap CL
        super(clazz);
        classContext = vm.getCompilationContext().getBootstrapClassContext();
    }

    VmClassLoaderImpl(VmClassLoaderClassImpl clazz, CompilationContext ctxt) {
        // non-bootstrap CL
        super(clazz);
        classContext = ctxt.constructClassContext(this);
    }

    @Override
    public ClassContext getClassContext() {
        return classContext;
    }

    // internal for VM bootstrap
    void registerClass(String name, VmClassImpl clazz) {
        defined.put(name, clazz);
    }

    @Override
    public VmClassLoaderClassImpl getVmClass() {
        return (VmClassLoaderClassImpl) super.getVmClass();
    }

    @Override
    public VmClassImpl loadClass(String name) throws Thrown {
        Assert.checkNotNullParam("name", name);
        VmClassImpl clazz = defined.get(name);
        if (clazz == null) {
            clazz = loadNewClass((VmThreadImpl) Vm.requireCurrentThread(), VmImpl.require().intern(name));
            VmClassImpl appearing = defined.putIfAbsent(name, clazz);
            if (appearing != null) {
                clazz = appearing;
            }
        }
        return clazz;
    }

    VmClassImpl loadClassRunTime(String name) throws Thrown {
        try {
            return loadClass(name);
        } catch (Thrown thrown) {
            VmImpl vm = VmImpl.require();
            VmThrowable throwable = vm.noClassDefFoundErrorClass.newInstance("Class definition not found", thrown.getThrowable());
            VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
            thread.setThrown(throwable);
            throw new Thrown(throwable);
        }
    }

    VmClassImpl findLoadedClass(String name) {
        return defined.get(name);
    }

    public VmClassImpl defineClass(VmString name, VmArray content, VmObject protectionDomain) throws Thrown {
        return defineClass(name, content, protectionDomain, false);
    }

    public VmClassImpl defineClass(VmString name, VmArray content, VmObject protectionDomain, boolean hidden) throws Thrown {
        VmImpl vm = VmImpl.require();
        String internalName = name.getContent();
        if (! hidden && defined.containsKey(internalName)) {
            throw duplicateClass(vm);
        }
        MemoryImpl memory = (MemoryImpl) content.getMemory();
        ClassFile classFile = ClassFile.of(classContext, ByteBuffer.wrap(memory.getArray(), content.getArrayElementOffset(0), content.getLength()));
        // todo: proper verification...
        DefinedTypeDefinition.Builder builder = classContext.newTypeBuilder();
        classFile.accept(builder);
        if (hidden) {
            builder.addModifiers(ClassFile.I_ACC_HIDDEN);
        }
        DefinedTypeDefinition defined = builder.build();
        if (! hidden) {
            classContext.defineClass(internalName, defined);
        }
        LoadedTypeDefinition loaded = defined.load();
        VmClassImpl vmClass = createVmClass(protectionDomain, vm, loaded, hidden);
        if (! hidden && this.defined.putIfAbsent(internalName, vmClass) != null) {
            VmThrowable throwable = vm.noClassDefFoundErrorClass.newInstance("Class already defined");
            VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
            thread.setThrown(throwable);
            throw new Thrown(throwable);
        }
        DefinedTypeDefinition nestHost = loaded.getNestHost();
        if (nestHost != null && nestHost.load() != loaded) {
            // real nest host
            vmClass.setNestHost((VmClassImpl) nestHost.load().getVmClass());
        }
        DefinedTypeDefinition[] nestMembers = loaded.getNestMembers();
        if (nestMembers != null && nestMembers.length > 0) {
            for (DefinedTypeDefinition nestMember : nestMembers) {
                LoadedTypeDefinition loadedMember = nestMember.load();
                if (loadedMember != loaded) {
                    vmClass.addNestMember((VmClassImpl) loadedMember.getVmClass());
                }
            }
        }
        return vmClass;
    }

    public VmClassImpl getOrDefineClass(LoadedTypeDefinition loaded) {
        String internalName = loaded.getInternalName();
        VmClassImpl vmClass = defined.get(internalName);
        if (vmClass == null) {
            VmImpl vm = getVmClass().getVm();
            vmClass = createVmClass(null, vm, loaded, false);
            VmClassImpl appearing = defined.putIfAbsent(internalName, vmClass);
            if (appearing != null) {
                vmClass = appearing;
            }
        }
        return vmClass;
    }

    private VmClassImpl createVmClass(final VmObject protectionDomain, final VmImpl vm, final LoadedTypeDefinition loaded, boolean hidden) {
        ObjectType type = loaded.getType();
        ClassObjectType classLoaderType = vm.classLoaderClass.getTypeDefinition().getClassType();
        ClassObjectType throwableType = vm.throwableClass.getTypeDefinition().getClassType();
        ClassObjectType threadType = vm.threadClass.getTypeDefinition().getClassType();
        // here is where we decide what kind of VmClass we're producing; note that some kinds of class cannot be defined
        VmClassImpl vmClass;
        if (type.isSubtypeOf(classLoaderType)) {
            vmClass = new VmClassLoaderClassImpl(vm, loaded, protectionDomain);
        } else if (type.isSubtypeOf(throwableType)) {
            vmClass = new VmThrowableClassImpl(vm, loaded, protectionDomain);
        } else if (type.isSubtypeOf(threadType)) {
            vmClass = new VmThreadClassImpl(vm, loaded, protectionDomain);
        } else {
            vmClass = new VmClassImpl(vm, loaded, protectionDomain);
        }
        vmClass.postConstruct(vm);
        return vmClass;
    }

    private Thrown duplicateClass(final VmImpl vm) {
        VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
        VmThrowable throwable = vm.linkageErrorClass.newInstance("Attempted duplicate class definition");
        thread.setThrown(throwable);
        return new Thrown(throwable);
    }

    VmClassImpl loadNewClass(VmThreadImpl thread, VmString intName) {
        VmClassImpl classLoaderClass = getVmClass();
        LoadedTypeDefinition clDef = classLoaderClass.getTypeDefinition();
        ClassContext classContext = this.classContext;
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        MethodDescriptor loadClassDesc = MethodDescriptor.synthesize(classContext, classDesc, List.of(stringDesc));
        if (classContext == classContext.getCompilationContext().getBootstrapClassContext()) {
            // skip JVM call
            DefinedTypeDefinition definedType = classContext.findDefinedType(intName.getContent());
            if (definedType == null) {
                VmThrowable throwable = thread.getVM().noClassDefFoundErrorClass.newInstance("Class not found: " + intName.getContent());
                thread.setThrown(throwable);
                throw new Thrown(throwable);
            }
            try {
                return (VmClassImpl) definedType.load().getVmClass();
            } catch (Exception e) {
                VmThrowable throwable = thread.getVM().noClassDefFoundErrorClass.newInstance("Class load failed: " + intName.getContent());
                thread.setThrown(throwable);
                throw new Thrown(throwable);
            }
        }
        return (VmClassImpl) classLoaderClass.getOrCompile(clDef.resolveMethodElementVirtual("loadClass", loadClassDesc)).invoke(thread, this, List.of(intName));
    }

    int getHiddenClassSeq(final String baseName) {
        return hiddenClassSeqMap.computeIfAbsent(baseName, VmClassLoaderImpl::newSeq).getAndIncrement();
    }

    private static AtomicInteger newSeq(final String ignored) {
        return new AtomicInteger(1);
    }
}
