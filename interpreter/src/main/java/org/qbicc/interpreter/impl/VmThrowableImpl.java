package org.qbicc.interpreter.impl;

import org.qbicc.context.ProgramLocatable;
import org.qbicc.graph.Value;
import org.qbicc.interpreter.Memory;
import org.qbicc.interpreter.Vm;
import org.qbicc.interpreter.VmClass;
import org.qbicc.interpreter.VmObject;
import org.qbicc.interpreter.VmThrowable;
import org.qbicc.plugin.layout.Layout;
import org.qbicc.plugin.layout.LayoutInfo;
import org.qbicc.type.definition.DefinedTypeDefinition;
import org.qbicc.type.definition.LoadedTypeDefinition;
import org.qbicc.type.definition.element.ConstructorElement;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.FieldElement;
import org.qbicc.type.definition.element.InitializerElement;
import org.qbicc.type.definition.element.MethodElement;

import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

final class VmThrowableImpl extends VmObjectImpl implements VmThrowable {
    private volatile ProgramLocatable[] backTrace = Value.NO_VALUES;

    VmThrowableImpl(VmThrowableClassImpl clazz) {
        super(clazz);
    }

    VmThrowableImpl(final VmThrowableImpl original) {
        super(original);
        backTrace = original.backTrace;
    }

    void captureBacktrace() {
        Frame currentFrame = ((VmThreadImpl) Vm.requireCurrentThread()).currentFrame;
        if (currentFrame == null) {
            // no stack :(
        } else {
            backTrace = currentFrame.getBackTrace();
        }
        Memory memory = getMemory();
        LoadedTypeDefinition throwableClassDef = ((VmImpl)Vm.requireCurrent()).throwableClass.getTypeDefinition();
        Layout interpLayout = Layout.get(throwableClassDef.getContext().getCompilationContext());
        LayoutInfo layout = interpLayout.getInstanceLayoutInfo(throwableClassDef);
        int depthIdx = layout.getMember(throwableClassDef.findField("depth")).getOffset();
        memory.store32(depthIdx, backTrace.length, SinglePlain);
    }

    void fillInStackTrace() {
        // no operation; done in captureBacktrace so the interpreter can create common exceptions without the overhead of interpreting their constructor chain
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
        ProgramLocatable[] backTrace = this.backTrace;
        VmObject[] steArray = ((VmRefArrayImpl) array).getArray();
        for (int i = 0; i < backTrace.length; i++) {
            ProgramLocatable ip = backTrace[i];
            ExecutableElement frameElement = ip.element();
            VmObjectImpl ste = vm.stackTraceElementClass.newInstance();
            vm.manuallyInitialize(ste);
            Memory steMemory = ste.getMemory();
            // initialize the stack trace element
            DefinedTypeDefinition frameClassDef = frameElement.getEnclosingType();
            VmClassImpl frameClass = vm.getClassLoaderForContext(frameClassDef.getContext()).getOrDefineClass(frameClassDef.load());
            steMemory.storeRef(declaringClassObjectIdx, frameClass, SinglePlain);
            steMemory.store32(lineNumberIdx, ip.lineNumber(), SinglePlain);
            if (frameElement.getSourceFileName() != null) {
                steMemory.storeRef(fileNameIdx, vm.intern(frameElement.getSourceFileName()), SinglePlain);
            }
            steMemory.storeRef(declaringClassIdx, vm.intern(frameClass.getName()), SinglePlain);
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
            steMemory.storeRef(methodNameIdx, vm.intern(methodName), SinglePlain);
            steArray[i] = ste;
        }
    }

    @Override
    public VmThrowableClassImpl getVmClass() {
        return (VmThrowableClassImpl) super.getVmClass();
    }

    public void prepareForSerialization() {
        VmImpl vm = this.getVmClass().getVm();
        LoadedTypeDefinition throwableClassDef = vm.throwableClass.getTypeDefinition();
        Layout layout = Layout.get(throwableClassDef.getContext().getCompilationContext());
        FieldElement stackTraceField = throwableClassDef.findField("stackTrace");
        FieldElement unassignedStack = throwableClassDef.findField("UNASSIGNED_STACK");
        int stOffset = layout.getInstanceLayoutInfo(throwableClassDef).getMember(stackTraceField).getOffset();
        int usOffset = layout.getStaticLayoutInfo(throwableClassDef).getMember(unassignedStack).getOffset();
        VmObject unassigned = vm.throwableClass.getStaticMemory().loadRef(usOffset, SinglePlain);
        Memory memory = getMemory();
        if (memory.loadRef(stOffset, SinglePlain) == unassigned) {
            VmArrayImpl stackTrace = (VmArrayImpl) vm.newArrayOf(vm.stackTraceElementClass, backTrace.length);
            initStackTraceElements(stackTrace);
            memory.storeRef(stOffset, stackTrace, SinglePlain);
        }
    }

    public String getMessage() {
        VmClassImpl vmClass = getVmClass().getVm().throwableClass;
        int offs = vmClass.getLayoutInfo().getMember(vmClass.getTypeDefinition().findField("detailMessage")).getOffset();
        VmStringImpl messageStr = (VmStringImpl) getMemory().loadRef(offs, SinglePlain);
        return messageStr == null ? null : messageStr.getContent();
    }

    public VmThrowable getCause() {
        VmClassImpl thr = getVmClass().getVm().throwableClass;
        int offs = thr.getLayoutInfo().getMember(thr.getTypeDefinition().findField("cause")).getOffset();
        VmThrowable cause = (VmThrowable)getMemory().loadRef(offs, SinglePlain);
        if (cause == null || cause.equals(this)) {
            return null; // Cyclic cause indicated Throwable still being initialized; suppress.
        } else {
            return cause;
        }
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        ProgramLocatable[] backTrace = this.backTrace;
        StackTraceElement[] stackTrace = new StackTraceElement[backTrace.length];
        for (int i = 0; i < backTrace.length; i++) {
            ProgramLocatable ip = backTrace[i];
            ExecutableElement frameElement = ip.element();
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
            int lineNumber = ip.lineNumber();
            stackTrace[i] = new StackTraceElement(classLoaderName, moduleName, moduleVersion, declaringClass, methodName, fileName, lineNumber);
        }
        return stackTrace;
    }

    @Override
    protected VmThrowableImpl clone() {
        return new VmThrowableImpl(this);
    }
}
