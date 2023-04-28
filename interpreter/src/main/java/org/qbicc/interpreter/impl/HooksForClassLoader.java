package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.SingleRelease;

import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmString;
import org.qbicc.interpreter.VmThread;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.FieldElement;

import java.util.List;

/**
 *
 */
final class HooksForClassLoader {
    private final FieldElement classDataField;
    private final VmClassImpl byteArrayInputStreamClass;
    private final ConstructorElement byteArrayInputStreamConstructor;

    HooksForClassLoader(VmImpl vm) {
        this.classDataField = vm.classClass.getTypeDefinition().findField("classData");
        this.byteArrayInputStreamClass = vm.getBootstrapClassLoader().loadClass("java/io/ByteArrayInputStream");
        this.byteArrayInputStreamConstructor = byteArrayInputStreamClass.getTypeDefinition().requireSingleConstructor(ce -> ce.getParameters().size() == 1);
    }

    @Hook
    static VmClass defineClass1(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmString className, VmByteArrayImpl bytes, int off, int len, VmObject pd, VmString source) {
        VmString name = thread.vm.fixClassname(className);
        if (off != 0 || len != bytes.getLength()) {
            bytes = bytes.copyOfRange(off, len);
        }
        if (classLoader == null) {
            classLoader = thread.vm.bootstrapClassLoader;
        }
        return classLoader.defineClass(name, bytes, pd);
    }

    @Hook
    VmClass defineClass0(VmThreadImpl thread, VmClassLoaderImpl classLoader, VmClassImpl lookup, VmString className, VmByteArrayImpl bytes, int off, int len, VmObject pd, boolean initialize, int flags, VmObject data) {
        VmString name = thread.vm.fixClassname(className);
        if (off != 0 || len != bytes.getLength()) {
            bytes = bytes.copyOfRange(off, len);
        }
        if (classLoader == null) {
            classLoader = thread.vm.bootstrapClassLoader;
        }
        boolean nestMate = (flags & 1) != 0;
        boolean hidden = (flags & 2) != 0;
        VmClassImpl defined = classLoader.defineClass(name, bytes, hidden);
        if (nestMate) {
            VmClassImpl host = lookup.getNestHost();
            host.addNestMember(defined);
            defined.setNestHost(host);
        }
        defined.getMemory().storeRef(defined.indexOf(classDataField), data, SingleRelease);
        if (initialize) {
            defined.initialize(thread);
        }
        return defined;
    }

    @Hook
    static VmClass findBootstrapClass(VmThread thread, VmString name) {
        final CompilationContext ctxt = thread.getVM().getCompilationContext();
        DefinedTypeDefinition definedType = ctxt.getBootstrapClassContext().findDefinedType(name.getContent().replace('.', '/'));
        return definedType == null ? null : definedType.load().getVmClass();
    }

    @Hook
    static VmClass findLoadedClass0(VmThread thread, VmClassLoaderImpl classLoader, VmString name) {
        return classLoader.findLoadedClass(name.getContent());
    }
}
