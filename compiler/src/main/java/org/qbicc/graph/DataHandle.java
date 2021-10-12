package org.qbicc.graph;

import org.qbicc.object.Data;
import org.qbicc.object.Function;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a data element.
 */
public final class DataHandle extends AbstractProgramObjectHandle {

    DataHandle(ExecutableElement element, int line, int bci, Data data) {
        super(element, line, bci, data);
    }

    @Override
    public boolean equals(AbstractProgramObjectHandle other) {
        return other instanceof DataHandle && equals((DataHandle) other);
    }

    public boolean equals(final DataHandle other) {
        return super.equals(other);
    }

    @Override
    public Function getProgramObject() {
        return (Function) super.getProgramObject();
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.NONE;
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "Data";
    }
}
