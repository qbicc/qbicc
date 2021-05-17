package org.qbicc.graph;

import org.qbicc.object.ProgramObject;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
public abstract class AbstractProgramObjectHandle extends AbstractValueHandle {
    private final ProgramObject programObject;

    AbstractProgramObjectHandle(ExecutableElement element, int line, int bci, ProgramObject programObject) {
        super(null, element, line, bci);
        this.programObject = programObject;
    }

    @Override
    public PointerType getPointerType() {
        return programObject.getType().getPointer();
    }

    @Override
    int calcHashCode() {
        return programObject.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof AbstractProgramObjectHandle && equals((AbstractProgramObjectHandle) other);
    }

    public boolean equals(AbstractProgramObjectHandle other) {
        return this == other || other != null && programObject.equals(other.programObject);
    }

    @Override
    public ValueType getValueType() {
        return programObject.getType();
    }

    public ProgramObject getProgramObject() {
        return programObject;
    }
}
