package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.VariableElement;

/**
 *
 */
public abstract class Variable extends AbstractValueHandle {
    private final VariableElement variableElement;
    private final PointerType pointerType;

    Variable(final ExecutableElement element, final int line, final int bci, final VariableElement variableElement, PointerType pointerType) {
        super(null, element, line, bci);
        this.variableElement = variableElement;
        this.pointerType = pointerType;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), variableElement);
    }

    public VariableElement getVariableElement() {
        return variableElement;
    }

    public PointerType getPointerType() {
        return pointerType;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return variableElement.hasAllModifiersOf(ClassFile.ACC_VOLATILE) ? MemoryAtomicityMode.VOLATILE : MemoryAtomicityMode.UNORDERED;
    }
}
