package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.qbicc.plugin.coreclasses.CoreClasses;
import org.qbicc.type.ClassObjectType;
import org.qbicc.type.ObjectType;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.VerifyFailedException;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.descriptor.ClassTypeDescriptor;
import org.qbicc.type.descriptor.MethodDescriptor;
import org.qbicc.type.methodhandle.MethodHandleConstant;

final class VmClassLoaderImpl extends VmObjectImpl implements VmClassLoader {
    private static final ThreadLocal<MessageDigest> SHA_256 = ThreadLocal.withInitial(() -> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    });

    private final ClassContext classContext;
    private final Map<String, VmClassImpl> defined = new ConcurrentHashMap<>();
    final ConcurrentHashMap<MethodDescriptor, VmObject> methodTypeCache = new ConcurrentHashMap<>();
    final ConcurrentHashMap<MethodHandleConstant, VmObject> methodHandleCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<byte[], AtomicInteger>> hiddenClassSeqMap = new ConcurrentHashMap<>();
    private final boolean specialCl;
    // protected by lock ↓
    private final HashMap<String, VmObjectImpl> modulesByPackage = new HashMap<>();
    private final HashMap<String, List<VmClassImpl>> classesWithoutModuleByPackage = new HashMap<>();
    // protected by lock ↑
    private String name;
    private static final AtomicInteger seq = new AtomicInteger(0);

    VmClassLoaderImpl(VmClassLoaderClassImpl clazz, VmImpl vm) {
        // bootstrap CL
        super(clazz);
        classContext = vm.getCompilationContext().getBootstrapClassContext();
        specialCl = true;
        name = "boot";
    }

    VmClassLoaderImpl(VmClassLoaderClassImpl clazz, CompilationContext ctxt) {
        // non-bootstrap CL
        super(clazz);
        // maybe app CL?
        if (clazz.getTypeDefinition().internalNameEquals("jdk/internal/loader/ClassLoaders$AppClassLoader")) {
            classContext = ctxt.constructAppClassLoaderClassContext(this);
            specialCl = true;
        } else if (clazz.getTypeDefinition().internalNameEquals("jdk/internal/loader/ClassLoaders$PlatformClassLoader")) {
            classContext = ctxt.constructPlatformClassContext(this);
            specialCl = true;
        } else if (clazz.getTypeDefinition().internalNameEquals("jdk/internal/loader/ClassLoaders$BootClassLoader")) {
            classContext = ctxt.getBootstrapClassContext();
            specialCl = true;
            name = "boot";
        } else {
            classContext = ctxt.constructClassContext(this);
            specialCl = false;
        }
    }

    @Override
    public String getName() {
        String name = this.name;
        if (name == null) {
            final VmString vmString = (VmString) getMemory().loadRef(indexOf(clazz.getVm().classLoaderClass.getTypeDefinition().findField("name")), SinglePlain);
            if (vmString == null) {
                this.name = name = "Unnamed" + seq.getAndIncrement();
            } else {
                this.name = name = vmString.getContent();
            }
        }
        return name;
    }

    @Override
    public ClassContext getClassContext() {
        if (classContext == null) {
            // special case: recursive initialization
            return getVmClass().getVm().getCompilationContext().getClassContextForLoader(this);
        }
        return classContext;
    }

    // internal for VM bootstrap
    void registerClass(String name, VmClassImpl clazz) {
        defined.put(name, clazz);
        initializeModule(clazz);
    }

    @Override
    public VmClassLoaderClassImpl getVmClass() {
        return (VmClassLoaderClassImpl) super.getVmClass();
    }

    @Override
    public VmClassImpl loadClass(String name) throws Thrown {
        Assert.checkNotNullParam("name", name);
        final String intName = name.replace('.', '/');
        VmClassImpl clazz = defined.get(intName);
        if (clazz == null) {
            clazz = loadNewClass((VmThreadImpl) Vm.requireCurrentThread(), VmImpl.require().intern(name));
            VmClassImpl appearing = defined.putIfAbsent(intName, clazz);
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
            throw new Thrown(throwable);
        }
    }

    VmClassImpl findLoadedClass(String name) {
        return defined.get(name.replace('.', '/'));
    }

    public VmClassImpl defineClass(VmString name, VmArray content, VmObject protectionDomain) throws Thrown {
        return defineClass(name, content, false);
    }

    public VmClassImpl defineClass(VmString name, VmArray content, boolean hidden) throws Thrown {
        VmImpl vm = VmImpl.require();
        String internalName = name.getContent();
        if (! hidden && defined.containsKey(internalName)) {
            throw duplicateClass(vm);
        }
        byte[] array = ((VmByteArrayImpl) content).getArray();
        ClassFile classFile = ClassFile.of(classContext, ByteBuffer.wrap(array));
        // todo: proper verification...
        DefinedTypeDefinition.Builder builder = classContext.newTypeBuilder();
        classFile.accept(builder);
        if (hidden) {
            byte[] digest = SHA_256.get().digest(array);
            builder.setDigest(digest);
            int seq = getHiddenClassSeq(internalName, digest);
            builder.setHiddenClassIndex(seq);
            builder.addModifiers(ClassFile.I_ACC_HIDDEN | ClassFile.I_ACC_NO_RESOLVE);
        }
        DefinedTypeDefinition defined = builder.build();
        // TODO: ↓↓ temporary ↓↓
        Path outputFile = classContext.getCompilationContext().getOutputFile(defined, "class");
        try {
            Files.write(outputFile, array);
        } catch (IOException ignored) {
        }
        // TODO: ↑↑ temporary ↑↑
        if (defined.hasNoModifiersOf(ClassFile.I_ACC_NO_RESOLVE)) {
            classContext.defineClass(internalName, defined);
        }
        LoadedTypeDefinition loaded;
        try {
            loaded = defined.load();
        } catch (VerifyFailedException e) {
            VmThrowable throwable = vm.verifyErrorClass.newInstance(e.getMessage());
            VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
            throw new Thrown(throwable);
        }
        VmClassImpl vmClass = createVmClass(vm, loaded, hidden);
        loaded.setVmClass(vmClass);
        if (! hidden && this.defined.putIfAbsent(internalName, vmClass) != null) {
            VmThrowable throwable = vm.noClassDefFoundErrorClass.newInstance("Class already defined");
            VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
            throw new Thrown(throwable);
        }
        initNestHost(loaded, vmClass);
        return vmClass;
    }

    private void initNestHost(final LoadedTypeDefinition loaded, final VmClassImpl vmClass) {
        DefinedTypeDefinition nestHost = loaded.getNestHost();
        if (nestHost != null) {
            // real nest host
            for (;;) {
                DefinedTypeDefinition innerHost = nestHost.load().getNestHost();
                if (innerHost != null && innerHost != nestHost) {
                    nestHost = innerHost;
                } else {
                    break;
                }
            }
            VmClassImpl hostClass = ((VmClassImpl) nestHost.load().getVmClass()).getNestHost();
            vmClass.setNestHost(hostClass);
            hostClass.addNestMember(vmClass);
        } else {
            vmClass.setNestHost(vmClass);
        }
    }

    public VmClassImpl getOrDefineClass(LoadedTypeDefinition loaded) {
        if (loaded.isHidden()) {
            throw new IllegalArgumentException("Cannot getOrDefineClass for a hidden class");
        }
        String internalName = loaded.getInternalName();
        VmClassImpl vmClass = defined.get(internalName);
        if (vmClass == null) {
            VmImpl vm = getVmClass().getVm();
            vmClass = createVmClass(vm, loaded, false);
            VmClassImpl appearing = defined.putIfAbsent(internalName, vmClass);
            if (appearing != null) {
                vmClass = appearing;
            } else {
                vmClass.initVmClass();
                initNestHost(loaded, vmClass);
            }
        }
        return vmClass;
    }

    void setModulePackage(String packageName, VmObjectImpl module) {
        packageName = packageName.replace('.', '/');
        synchronized (this) {
            VmObjectImpl existing = modulesByPackage.putIfAbsent(packageName, module);
            if (existing == module) {
                // OK idempotency
                return;
            }
            if (existing != null) {
                throw new IllegalStateException("Package defined to more than one module: " + packageName);
            }
            // also update any classes defined to this package
            List<VmClassImpl> list = classesWithoutModuleByPackage.remove(packageName);
            if (list != null) {
                for (VmClassImpl vmClass : list) {
                    vmClass.setModule(module);
                }
            }
        }
    }

    private VmClassImpl createVmClass(final VmImpl vm, final LoadedTypeDefinition loaded, boolean hidden) {
        ObjectType type = loaded.getObjectType();
        ClassObjectType classLoaderType = vm.classLoaderClass.getTypeDefinition().getClassType();
        ClassObjectType throwableType = vm.throwableClass.getTypeDefinition().getClassType();
        ClassObjectType threadType = vm.threadClass.getTypeDefinition().getClassType();
        // here is where we decide what kind of VmClass we're producing; note that some kinds of class cannot be defined
        VmClassImpl vmClass;
        if (type.isSubtypeOf(classLoaderType)) {
            vmClass = new VmClassLoaderClassImpl(vm, loaded);
        } else if (type.isSubtypeOf(throwableType)) {
            vmClass = new VmThrowableClassImpl(vm, loaded);
        } else if (type.isSubtypeOf(threadType)) {
            vmClass = new VmThreadClassImpl(vm, loaded);
        } else {
            vmClass = new VmClassImpl(vm, loaded);
        }
        vmClass.postConstruct(vm);
        initializeModule(vmClass);
        return vmClass;
    }

    void initializeModule(final VmClassImpl vmClass) {
        String packageName = vmClass.getPackageInternalName();
        // calculate the module for this class
        synchronized (this) {
            VmObjectImpl module = modulesByPackage.get(packageName);
            if (module == null) {
                module = getUnnamedModule();
            }
            if (module != null) {
                vmClass.setModule(module);
            } else {
                classesWithoutModuleByPackage.computeIfAbsent(packageName, VmClassLoaderImpl::newList).add(vmClass);
            }
        }
    }

    private static <E> List<E> newList(final Object ignored) {
        return new ArrayList<>();
    }

    private VmObjectImpl getUnnamedModule() {
        CoreClasses coreClasses = CoreClasses.get(getVmClass().getVm().getCompilationContext());
        return (VmObjectImpl) getMemory().loadRef(indexOf(coreClasses.getClassLoaderUnnamedModuleField()), SinglePlain);
    }

    private Thrown duplicateClass(final VmImpl vm) {
        VmThreadImpl thread = (VmThreadImpl) Vm.requireCurrentThread();
        VmThrowable throwable = vm.linkageErrorClass.newInstance("Attempted duplicate class definition");
        return new Thrown(throwable);
    }

    VmClassImpl loadNewClass(VmThreadImpl thread, VmString name) {
        VmClassImpl classLoaderClass = getVmClass();
        LoadedTypeDefinition clDef = classLoaderClass.getTypeDefinition();
        ClassContext classContext = this.classContext;
        ClassTypeDescriptor stringDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/String");
        ClassTypeDescriptor classDesc = ClassTypeDescriptor.synthesize(classContext, "java/lang/Class");
        MethodDescriptor loadClassDesc = MethodDescriptor.synthesize(classContext, classDesc, List.of(stringDesc));
        if (specialCl) {
            // skip JVM call
            DefinedTypeDefinition definedType = classContext.findDefinedType(name.getContent().replace('.', '/'));
            if (definedType == null) {
                VmThrowable throwable = thread.getVM().classNotFoundExceptionClass.newInstance("Class not found: " + name.getContent());
                throw new Thrown(throwable);
            }
            try {
                return (VmClassImpl) definedType.load().getVmClass();
            } catch (Exception e) {
                VmThrowable throwable = thread.getVM().noClassDefFoundErrorClass.newInstance("Class load failed: " + name.getContent());
                throw new Thrown(throwable);
            }
        }
        return (VmClassImpl) classLoaderClass.getOrCompile(clDef.resolveMethodElementVirtual(classContext.getCompilationContext().getBootstrapClassContext(), "loadClass", loadClassDesc)).invoke(thread, this, List.of(name));
    }

    int getHiddenClassSeq(final String internalName, final byte[] digest) {
        return hiddenClassSeqMap.computeIfAbsent(internalName, VmClassLoaderImpl::newTreeMap).computeIfAbsent(digest, VmClassLoaderImpl::newSeq).getAndIncrement();
    }

    private static <V> Map<byte[], V> newTreeMap(final Object ignored) {
        return Collections.synchronizedMap(new TreeMap<>(Arrays::compare));
    }

    private static AtomicInteger newSeq(final Object ignored) {
        return new AtomicInteger(1);
    }
}
