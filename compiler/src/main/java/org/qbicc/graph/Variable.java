package org.qbicc.graph;

import java.util.Objects;

import org.qbicc.graph.atomic.AccessMode;
import org.qbicc.type.PointerType;
import org.qbicc.type.definition.classfile.ClassFile;
import org.qbicc.type.definition.element.ExecutableElement;
import org.qbicc.type.definition.element.VariableElement;

import static org.qbicc.graph.atomic.AccessModes.GlobalSeqCst;
import static org.qbicc.graph.atomic.AccessModes.SinglePlain;

/**
 *
 */
public abstract class Variable extends AbstractPointerValue {
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

    @Override
    public StringBuilder toString(StringBuilder b) {
        super.toString(b);
        b.append('{');
        // todo: replace with variableElement.toString(b)
        b.append(variableElement);
        b.append('}');
        return b;
    }

    public VariableElement getVariableElement() {
        return variableElement;
    }

    public PointerType getType() {
        return pointerType;
    }

    @Override
    public AccessMode getDetectedMode() {
        return variableElement.hasAllModifiersOf(ClassFile.ACC_VOLATILE) ? GlobalSeqCst : SinglePlain;
    }
}
