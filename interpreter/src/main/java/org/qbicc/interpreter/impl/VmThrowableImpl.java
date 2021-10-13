package org.qbicc.interpreter.impl;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

final class VmThrowableImpl extends VmObjectImpl implements VmThrowable {
    private volatile Node[] backTrace = Value.NO_VALUES;

    VmThrowableImpl(VmThrowableClassImpl clazz) {
        super(clazz);
    }

    void initializeDepth() {
        MemoryImpl memory = getMemory();
        LoadedTypeDefinition throwableClassDef = ((VmImpl)Vm.requireCurrent()).throwableClass.getTypeDefinition();
        Layout interpLayout = Layout.getForInterpreter(throwableClassDef.getContext().getCompilationContext());
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(throwableClassDef);
        int depthIdx = layout.getMember(throwableClassDef.findField("depth")).getOffset();
        memory.store32(depthIdx, backTrace.length, MemoryAtomicityMode.UNORDERED);
    }

    void fillInStackTrace() {
        backTrace = ((VmThreadImpl)Vm.requireCurrentThread()).currentFrame.getBackTrace();
    }

    void initStackTraceElements(VmArrayImpl array) {
        VmImpl vm = getVmClass().getVm();
        Layout interpLayout = Layout.getForInterpreter(vm.getCompilationContext());
        // create the stack trace directly
        LoadedTypeDefinition steClassDef = vm.stackTraceElementClass.getTypeDefinition();
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(steClassDef);
        int declaringClassObjectIdx = layout.getMember(steClassDef.findField("declaringClassObject")).getOffset();
        int lineNumberIdx = layout.getMember(steClassDef.findField("lineNumber")).getOffset();
        int declaringClassIdx = layout.getMember(steClassDef.findField("declaringClass")).getOffset();
        int fileNameIdx = layout.getMember(steClassDef.findField("fileName")).getOffset();
        int methodNameIdx = layout.getMember(steClassDef.findField("methodName")).getOffset();
        for (int i = 0; i < backTrace.length; i++) {
            Node ip = backTrace[i];
            ExecutableElement frameElement = ip.getElement();
            VmObjectImpl ste = vm.stackTraceElementClass.newInstance();
            vm.manuallyInitialize(ste);
            MemoryImpl steMemory = ste.getMemory();
            // initialize the stack trace element
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
            array.getMemory().storeRef(array.getArrayElementOffset(i), ste, MemoryAtomicityMode.UNORDERED);
        }
    }

    @Override
    public VmThrowableClassImpl getVmClass() {
        return (VmThrowableClassImpl) super.getVmClass();
    }

    public String getMessage() {
        VmClassImpl vmClass = getVmClass().getVm().throwableClass;
        int offs = vmClass.getLayoutInfo().getMember(vmClass.getTypeDefinition().findField("detailMessage")).getOffset();
        VmStringImpl messageStr = (VmStringImpl) getMemory().loadRef(offs, MemoryAtomicityMode.UNORDERED);
        return messageStr == null ? null : messageStr.getContent();
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        VmImpl vm = getVmClass().getVm();
        MemoryImpl memory = getMemory();
        LoadedTypeDefinition throwableClassDef = vm.throwableClass.getTypeDefinition();
        Layout interpLayout = Layout.getForInterpreter(vm.getCompilationContext());
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(throwableClassDef);
        int stackTraceIdx = layout.getMember(throwableClassDef.findField("stackTrace")).getOffset();
        LoadedTypeDefinition steClassDef = vm.stackTraceElementClass.getTypeDefinition();
        layout = interpLayout.getInstanceLayoutInfo(steClassDef);
        int classLoaderNameIdx = layout.getMember(steClassDef.findField("classLoaderName")).getOffset();
        int moduleNameIdx = layout.getMember(steClassDef.findField("moduleName")).getOffset();
        int moduleVersionIdx = layout.getMember(steClassDef.findField("moduleVersion")).getOffset();
        int lineNumberIdx = layout.getMember(steClassDef.findField("lineNumber")).getOffset();
        int declaringClassIdx = layout.getMember(steClassDef.findField("declaringClass")).getOffset();
        int fileNameIdx = layout.getMember(steClassDef.findField("fileName")).getOffset();
        int methodNameIdx = layout.getMember(steClassDef.findField("methodName")).getOffset();
        VmArrayImpl array = (VmArrayImpl) memory.loadRef(stackTraceIdx, MemoryAtomicityMode.UNORDERED);
        int length = array.getLength();
        StackTraceElement[] stackTrace = new StackTraceElement[length];
        for (int i = 0; i < length; i ++) {
            memory = array.getMemory();
            VmObjectImpl steObject = (VmObjectImpl) memory.loadRef(array.getArrayElementOffset(i), MemoryAtomicityMode.UNORDERED);
            memory = steObject.getMemory();
            String classLoaderName = ((VmStringImpl)memory.loadRef(classLoaderNameIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            String moduleName = ((VmStringImpl)memory.loadRef(moduleNameIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            String moduleVersion = ((VmStringImpl)memory.loadRef(moduleVersionIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            String declaringClass = ((VmStringImpl)memory.loadRef(declaringClassIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            String methodName = ((VmStringImpl)memory.loadRef(methodNameIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            String fileName = ((VmStringImpl)memory.loadRef(fileNameIdx, MemoryAtomicityMode.UNORDERED)).getContent();
            int lineNumber = memory.load32(lineNumberIdx, MemoryAtomicityMode.UNORDERED);
            stackTrace[i] = new StackTraceElement(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
        }
        return stackTrace;
    }
}
