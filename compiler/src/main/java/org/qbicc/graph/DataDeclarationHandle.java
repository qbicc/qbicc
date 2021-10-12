package org.qbicc.graph;

import org.qbicc.object.DataDeclaration;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 * A handle for a data element.
 */
public final class DataDeclarationHandle extends AbstractProgramObjectHandle {

    DataDeclarationHandle(ExecutableElement element, int line, int bci, DataDeclaration dataDeclaration) {
        super(element, line, bci, dataDeclaration);
    }

    @Override
    public boolean equals(AbstractProgramObjectHandle other) {
        return other instanceof DataDeclarationHandle && equals((DataDeclarationHandle) other);
    }

    public boolean equals(final DataDeclarationHandle other) {
        return super.equals(other);
    }

    @Override
    public DataDeclaration getProgramObject() {
        return (DataDeclaration) super.getProgramObject();
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return MemoryAtomicityMode.NONE;
    }

    @Override
    public <T> long accept(ValueHandleVisitorLong<T> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public <T, R> R accept(ValueHandleVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    String getNodeName() {
        return "DataDeclaration";
    }
}
