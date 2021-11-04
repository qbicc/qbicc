package org.qbicc.graph;

import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.MethodDescriptor;

/**
 * A value handle to an executable element.
 */
public abstract class Executable extends AbstractValueHandle {
    private final ExecutableElement executable;
    private final MethodDescriptor callSiteDescriptor;
    private final FunctionType callSiteType;

    Executable(ExecutableElement currentElement, int line, int bci, ExecutableElement executable, MethodDescriptor callSiteDescriptor, FunctionType callSiteType) {
        super(null, currentElement, line, bci);
        this.executable = executable;
        this.callSiteDescriptor = callSiteDescriptor;
        this.callSiteType = callSiteType;
    }

    @Override
    int calcHashCode() {
        return executable.hashCode();
    }

    @Override
    public PointerType getPointerType() {
        return getValueType().getPointer();
    }

    @Override
    public FunctionType getValueType() {
        return executable.getType();
    }

    public ExecutableElement getExecutable() {
        return executable;
    }

    public MethodDescriptor getCallSiteDescriptor() {
        return callSiteDescriptor;
    }

    public FunctionType getCallSiteType() {
        return callSiteType;
    }

    @Override
    public boolean isNoThrow() {
        return executable.hasAllModifiersOf(ClassFile.I_ACC_NO_THROW);
    }

    @Override
    public boolean isNoReturn() {
        return executable.hasAllModifiersOf(ClassFile.I_ACC_NO_RETURN);
    }

    @Override
    public boolean isFold() {
        return executable.hasAllModifiersOf(ClassFile.I_ACC_FOLD);
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof Executable && equals((Executable) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('{');
        // todo: replace with executable.toString(b)
        b.append(executable);
        b.append('}');
        return b;
    }

    public boolean equals(Executable other) {
        return this == other || other != null && executable.equals(other.executable);
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public boolean isReadable() {
        return false;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.NONE;
    }
}
