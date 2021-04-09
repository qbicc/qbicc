package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.type.ValueType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.VariableElement;

/**
 *
 */
public abstract class Variable extends AbstractValueHandle {
    private final VariableElement variableElement;
    private final ValueType valueType;

    Variable(final ExecutableElement element, final int line, final int bci, final VariableElement variableElement, ValueType valueType) {
        super(null, element, line, bci);
        this.variableElement = variableElement;
        this.valueType = valueType;
    }

    int calcHashCode() {
        return Objects.hash(getClass(), variableElement);
    }

    public VariableElement getVariableElement() {
        return variableElement;
    }

    public ValueType getValueType() {
        return valueType;
    }

    @Override
    public MemoryAtomicityMode getDetectedMode() {
        return variableElement.hasAllModifiersOf(ClassFile.ACC_VOLATILE) ? MemoryAtomicityMode.VOLATILE : MemoryAtomicityMode.UNORDERED;
    }
}
