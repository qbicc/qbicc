package org.qbicc.interpreter.impl;

import org.qbicc.graph.MemoryAtomicityMode;
import org.qbicc.graph.Node;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
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

    VmThrowableImpl(final VmThrowableImpl original) {
        super(original);
        backTrace = original.backTrace;
    }

    void initializeDepth() {
        Frame currentFrame = ((VmThreadImpl) Vm.requireCurrentThread()).currentFrame;
        if (currentFrame == null) {
            // no stack :(
        } else {
            backTrace = currentFrame.getBackTrace();
        }
        MemoryImpl memory = getMemory();
        LoadedTypeDefinition throwableClassDef = ((VmImpl)Vm.requireCurrent()).throwableClass.getTypeDefinition();
        Layout interpLayout = Layout.get(throwableClassDef.getContext().getCompilationContext());
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(throwableClassDef);
        int depthIdx = layout.getMember(throwableClassDef.findField("depth")).getOffset();
        memory.store32(depthIdx, backTrace.length, MemoryAtomicityMode.UNORDERED);
    }

    void fillInStackTrace() {
        // no operation
    }

    void initStackTraceElements(VmArrayImpl array) {
        VmImpl vm = getVmClass().getVm();
        Layout interpLayout = Layout.get(vm.getCompilationContext());
        // create the stack trace directly
        LoadedTypeDefinition steClassDef = vm.stackTraceElementClass.getTypeDefinition();
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(steClassDef);
        int declaringClassObjectIdx = layout.getMember(steClassDef.findField("declaringClassObject")).getOffset();
        int lineNumberIdx = layout.getMember(steClassDef.findField("lineNumber")).getOffset();
        int declaringClassIdx = layout.getMember(steClassDef.findField("declaringClass")).getOffset();
        int fileNameIdx = layout.getMember(steClassDef.findField("fileName")).getOffset();
        int methodNameIdx = layout.getMember(steClassDef.findField("methodName")).getOffset();
        Node[] backTrace = this.backTrace;
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

    public VmThrowable getCause() {
        VmClassImpl thr = getVmClass().getVm().throwableClass;
        int offs = thr.getLayoutInfo().getMember(thr.getTypeDefinition().findField("cause")).getOffset();
        VmThrowable cause = (VmThrowable)getMemory().loadRef(offs, MemoryAtomicityMode.UNORDERED);
        if (cause == null || cause.equals(this)) {
            return null; // Cyclic cause indicated Throwable still being initialized; suppress.
        } else {
            return cause;
        }
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        Node[] backTrace = this.backTrace;
        StackTraceElement[] stackTrace = new StackTraceElement[backTrace.length];
        for (int i = 0; i < backTrace.length; i++) {
            Node ip = backTrace[i];
            ExecutableElement frameElement = ip.getElement();
            VmClass elementClass = frameElement.getEnclosingType().load().getVmClass();
            String classLoaderName = null; // todo fill in from frame element
            String moduleName = null; // todo fill in from frame element
            String moduleVersion = null; // todo fill in from frame element
            String declaringClass = elementClass.getName();
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
            String fileName = frameElement.getSourceFileName();
            int lineNumber = ip.getSourceLine();
            stackTrace[i] = new StackTraceElement(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
        }
        return stackTrace;
    }

    @Override
    protected VmThrowableImpl clone() {
        return new VmThrowableImpl(this);
    }
}
