package org.qbicc.graph;

import org.qbicc.type.FunctionType;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A value handle to an executable element.
 */
public abstract class Executable extends AbstractValueHandle {
    private final ExecutableElement executable;

    Executable(ExecutableElement currentElement, int line, int bci, ExecutableElement executable) {
        super(null, currentElement, line, bci);
        this.executable = executable;
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
