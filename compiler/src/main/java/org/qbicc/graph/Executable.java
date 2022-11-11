package org.qbicc.graph;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.InvokableType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.descriptor.MethodDescriptor;

import static org.qbicc.graph.atomic.AccessModes.SingleUnshared;

/**
 * A value handle to an executable element.
 */
public abstract class Executable extends AbstractValueHandle {
    private final ExecutableElement executable;
    private final MethodDescriptor callSiteDescriptor;
    private final InvokableType callSiteType;

    Executable(ExecutableElement currentElement, int line, int bci, ExecutableElement executable, MethodDescriptor callSiteDescriptor, InvokableType callSiteType) {
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
    public PointerType getType() {
        return getPointeeType().getPointer();
    }

    @Override
    public InvokableType getPointeeType() {
        return callSiteType;
    }

    public ExecutableElement getExecutable() {
        return executable;
    }

    public MethodDescriptor getCallSiteDescriptor() {
        return callSiteDescriptor;
    }

    public InvokableType getCallSiteType() {
        return callSiteType;
    }

    @Override
    public boolean isNoThrow() {
        return executable.hasAllModifiersOf(ClassFile.I_ACC_NO_THROW);
    }

    @Override
    public boolean isNoSafePoints() {
        return executable.hasAllModifiersOf(ClassFile.I_ACC_NO_SAFEPOINTS);
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
    public AccessMode getDetectedMode() {
        return SingleUnshared;
    }
}
