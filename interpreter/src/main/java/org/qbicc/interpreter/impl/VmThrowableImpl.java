package org.qbicc.interpreter.impl;

import java.lang.invoke.VarHandle;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

final class VmThrowableImpl extends VmObjectImpl implements VmThrowable {
    private volatile Frame stackTrace;

    VmThrowableImpl(VmClassImpl clazz) {
        super(clazz);
        VmThreadImpl vmThread = (VmThreadImpl) Vm.currentThread();
        if (vmThread != null) {
            // we have a stack then too
            stackTrace = vmThread.currentFrame;
        }
    }

    void fillInStackTrace() {
        VmImpl vm = getVmClass().getVm();
        MemoryImpl memory = getMemory();
        // compute the stack depth
        int depth = 0;
        Frame frame = stackTrace;
        while (frame != null) {
            Node ip = frame.ip;
            while (ip != null) {
                if (ip.getElement().hasNoModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    depth++;
                }
                ip = ip.getCallSite();
            }
            frame = frame.enclosing;
        }
        LoadedTypeDefinition throwableClassDef = vm.throwableClass.getTypeDefinition();
        Layout interpLayout = Layout.getForInterpreter(vm.getCompilationContext());
        Layout.LayoutInfo layout = interpLayout.getInstanceLayoutInfo(throwableClassDef);
        int depthIdx = layout.getMember(throwableClassDef.findField("depth")).getOffset();
        int stackTraceIdx = layout.getMember(throwableClassDef.findField("stackTrace")).getOffset();
        memory.store32(depthIdx, depth, MemoryAtomicityMode.UNORDERED);
        // create the stack trace directly
        LoadedTypeDefinition steClassDef = vm.stackTraceElementClass.getTypeDefinition();
        layout = interpLayout.getInstanceLayoutInfo(steClassDef);
        int declaringClassObjectIdx = layout.getMember(steClassDef.findField("declaringClassObject")).getOffset();
        int lineNumberIdx = layout.getMember(steClassDef.findField("lineNumber")).getOffset();
        int declaringClassIdx = layout.getMember(steClassDef.findField("declaringClass")).getOffset();
        int fileNameIdx = layout.getMember(steClassDef.findField("fileName")).getOffset();
        int methodNameIdx = layout.getMember(steClassDef.findField("methodName")).getOffset();
        VmArrayImpl array = vm.stackTraceElementClass.getArrayClass().newInstance(depth);
        depth = 0;
        frame = stackTrace;
        while (frame != null) {
            Node ip = frame.ip;
            while (ip != null) {
                if (ip.getElement().hasNoModifiersOf(ClassFile.I_ACC_HIDDEN)) {
                    VmObjectImpl ste = vm.stackTraceElementClass.newInstance();
                    MemoryImpl steMemory = ste.getMemory();
                    // initialize the stack trace element
                    ExecutableElement frameElement = ip.getElement();
                    DefinedTypeDefinition frameClassDef = frameElement.getEnclosingType();
                    VmClassImpl frameClass = vm.getClassLoaderForContext(frameClassDef.getContext()).getOrDefineClass(frameClassDef.load());
                    steMemory.storeRef(declaringClassObjectIdx, frameClass, MemoryAtomicityMode.UNORDERED);
                    steMemory.store32(lineNumberIdx, ip.getSourceLine(), MemoryAtomicityMode.UNORDERED);
                    steMemory.storeRef(fileNameIdx, vm.intern(frameElement.getSourceFileName()), MemoryAtomicityMode.UNORDERED);
                    steMemory.storeRef(declaringClassIdx, vm.intern(frameClass.getName()), MemoryAtomicityMode.UNORDERED);
                    String methodName;
                    if (frameElement instanceof MethodElement) {
                        methodName = ((MethodElement) frameElement).getName();
                    } else if (frameElement instanceof ConstructorElement) {
                        methodName = "<init>";
                    } else if (frameElement instanceof InitializerElement) {
                        methodName = "<clinit>";
                    } else {
                        methodName = "<unknown>";
                    }
                    steMemory.storeRef(methodNameIdx, vm.intern(methodName), MemoryAtomicityMode.UNORDERED);
                    array.getMemory().storeRef(array.getArrayElementOffset(depth), ste, MemoryAtomicityMode.UNORDERED);
                    depth++;
                }
                ip = ip.getCallSite();
            }
            frame = frame.enclosing;
        }
        memory.storeRef(stackTraceIdx, array, MemoryAtomicityMode.UNORDERED);
        VarHandle.releaseFence();
    }

    public String getMessage() {
        VmClassImpl vmClass = getVmClass().getVm().throwableClass;
        int offs = vmClass.getLayoutInfo().getMember(vmClass.getTypeDefinition().findField("detailMessage")).getOffset();
        VmStringImpl messageStr = (VmStringImpl) getMemory().loadRef(offs, MemoryAtomicityMode.UNORDERED);
        return messageStr == null ? null : messageStr.getContent();
    }
}
