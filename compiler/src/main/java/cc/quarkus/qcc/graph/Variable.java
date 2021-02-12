package cc.quarkus.qcc.graph;

import java.util.Objects;

import cc.quarkus.qcc.type.ValueType;
import cc.quarkus.qcc.type.definition.classfile.ClassFile;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import cc.quarkus.qcc.type.definition.element.VariableElement;

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
