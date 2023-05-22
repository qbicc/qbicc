package org.qbicc.interpreter.impl;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

import java.util.concurrent.atomic.AtomicInteger;

import org.qbicc.context.AttachmentKey;
import org.qbicc.context.CompilationContext;
import org.qbicc.interpreter.Hook;
import org.qbicc.interpreter.Thrown;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThread;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.pointer.GlobalPointer;
import org.qbicc.pointer.Pointer;
import org.qbicc.pointer.StaticFieldPointer;
import org.qbicc.type.StructType;
import org.qbicc.type.TypeSystem;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.GlobalVariableElement;
import org.qbicc.type.definition.element.StaticFieldElement;
import org.qbicc.type.descriptor.BaseTypeDescriptor;
import org.qbicc.type.generic.TypeSignature;

/**
 *
 */
final class HooksForUnsafe {
    private static final AttachmentKey<AtomicInteger> MEM_CNT_KEY = new AttachmentKey<>();

    HooksForUnsafe() {}

    @Hook
    static GlobalPointer allocateMemory0(VmThread thread, VmObject theUnsafe, long size) {
        CompilationContext ctxt = thread.getVM().getCompilationContext();
        final TypeSystem ts = ctxt.getTypeSystem();
        final AtomicInteger cnt = ctxt.computeAttachmentIfAbsent(MEM_CNT_KEY, AtomicInteger::new);
        final GlobalVariableElement.Builder builder = GlobalVariableElement.builder("direct_buf" + cnt.getAndIncrement(), BaseTypeDescriptor.V);
        builder.setType(ts.getArrayType(ts.getSignedInteger8Type(), size));
        // todo: buffer-specific section
        builder.setSection(ctxt.getImplicitSection());
        // it belongs to the caller of the caller of this method
        // todo: maybe, maybe not
        builder.setEnclosingType(((VmThreadImpl)thread).currentFrame.enclosing.enclosing.element.getEnclosingType());
        builder.setSignature(TypeSignature.synthesize(ctxt.getBootstrapClassContext(), builder.getDescriptor()));
        final GlobalVariableElement global = builder.build();
        return global.getPointer();
    }

    @Hook
    static void ensureClassInitialized(VmThreadImpl thread, VmObject theUnsafe, VmClassImpl clazz) {
        clazz.initialize(thread);
    }

    @Hook
    static boolean shouldBeInitialized0(VmThread thread, VmObject theUnsafe, VmClassImpl clazz) {
        return clazz.shouldBeInitialized();
    }

    @Hook
    static long objectFieldOffset0(VmThreadImpl thread, VmObject theUnsafe, VmObjectImpl fieldObj) {
        final VmImpl vm = thread.vm;
        VmClassImpl fieldClazz = vm.bootstrapClassLoader.loadClass("java/lang/reflect/Field");
        LoadedTypeDefinition fieldDef = fieldClazz.getTypeDefinition();
        VmClassImpl clazz = (VmClassImpl) fieldObj.getMemory().loadRef(fieldObj.indexOf(fieldDef.findField("clazz")), SinglePlain);
        VmStringImpl name = (VmStringImpl) fieldObj.getMemory().loadRef(fieldObj.indexOf(fieldDef.findField("name")), SinglePlain);
        LoadedTypeDefinition clazzDef = clazz.getTypeDefinition();
        FieldElement field = clazzDef.findField(name.getContent());
        if (field == null || field.isStatic()) {
            throw new Thrown(vm.errorClass.newInstance("Invalid argument to objectFieldOffset0"));
        }
        field.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        LayoutInfo layoutInfo = Layout.get(vm.getCompilationContext()).getInstanceLayoutInfo(clazzDef);
        StructType.Member member = layoutInfo.getMember(field);
        if (member == null) {
            throw new Thrown(vm.errorClass.newInstance("Internal error"));
        }
        return member.getOffset();
    }

    @Hook
    static long objectFieldOffset1(VmThreadImpl thread, VmObject theUnsafe, VmClassImpl clazz, VmStringImpl name) {
        final VmImpl vm = thread.vm;
        LoadedTypeDefinition clazzDef = clazz.getTypeDefinition();
        FieldElement field = clazzDef.findField(name.getContent());
        if (field == null || field.isStatic()) {
            throw new Thrown(vm.errorClass.newInstance("Invalid argument to objectFieldOffset1"));
        }
        field.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        LayoutInfo layoutInfo = Layout.get(vm.getCompilationContext()).getInstanceLayoutInfo(clazzDef);
        StructType.Member member = layoutInfo.getMember(field);
        if (member == null) {
            throw new Thrown(vm.errorClass.newInstance("Internal error"));
        }
        return member.getOffset();
    }

    @Hook
    static VmObject staticFieldBase0(VmThread thread, VmObject theUnsafe, VmObjectImpl fieldObj) {
        return null;
    }

    @Hook
    static StaticFieldPointer staticFieldOffset0(VmThreadImpl thread, VmObject theUnsafe, VmObjectImpl fieldObj) {
        final VmImpl vm = thread.vm;
        VmClassImpl fieldClazz = vm.bootstrapClassLoader.loadClass("java/lang/reflect/Field");
        LoadedTypeDefinition fieldDef = fieldClazz.getTypeDefinition();
        VmClassImpl clazz = (VmClassImpl) fieldObj.getMemory().loadRef(fieldObj.indexOf(fieldDef.findField("clazz")), SinglePlain);
        VmStringImpl name = (VmStringImpl) fieldObj.getMemory().loadRef(fieldObj.indexOf(fieldDef.findField("name")), SinglePlain);
        LoadedTypeDefinition clazzDef = clazz.getTypeDefinition();
        FieldElement field = clazzDef.findField(name.getContent());
        if (! (field instanceof StaticFieldElement sfe)) {
            throw new Thrown(vm.errorClass.newInstance("Invalid argument to objectFieldOffset0"));
        }
        sfe.setModifierFlags(ClassFile.I_ACC_NOT_REALLY_FINAL);
        return StaticFieldPointer.of(sfe);
    }

    @Hook
    static VmObject allocateInstance(VmThreadImpl thread, VmObject theUnsafe, VmClassImpl clazz) {
        final VmImpl vm = thread.vm;
        if (clazz.getTypeDefinition().isAbstract()) {
            VmThrowableClassImpl ie = (VmThrowableClassImpl) vm.bootstrapClassLoader.loadClass("java/lang/InstantiationException");
            throw new Thrown(ie.newInstance("Abstract class"));
        }
        return vm.manuallyInitialize(clazz.newInstance());
    }

    @Hook
    static void checkNativeAddress(VmThread thread, VmObject theUnsafe, Pointer address) {
        // make it a no-op at build-time
    }
}
