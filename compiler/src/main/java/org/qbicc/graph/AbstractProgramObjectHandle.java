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
        return programObject.getSymbolType();
    }

    @Override
    int calcHashCode() {
        return programObject.hashCode();
    }

    @Override
    public final boolean equals(Object other) {
        return other instanceof AbstractProgramObjectHandle && equals((AbstractProgramObjectHandle) other);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('(');
        programObject.getSymbolType().toString(b);
        b.append(' ');
        b.append('"');
        b.append(programObject.getName());
        b.append('"');
        b.append(')');
        return b;
    }

    public boolean equals(AbstractProgramObjectHandle other) {
        return this == other || other != null && programObject.equals(other.programObject);
    }

    @Override
    public ValueType getValueType() {
        return programObject.getValueType();
    }

    public boolean isConstantLocation() {
        return true;
    }

    @Override
    public boolean isValueConstant() {
        return true;
    }

    public ProgramObject getProgramObject() {
        return programObject;
    }
}
