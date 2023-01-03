package org.qbicc.graph;

import org.qbicc.type.VoidType;
import org.qbicc.type.definition.element.ExecutableElement;

/**
 *
 */
abstract class AbstractValue extends AbstractNode implements Value {
    private boolean usedInSuccessor;

    AbstractValue(final Node callSite, final ExecutableElement element, final int line, final int bci) {
        super(callSite, element, line, bci);
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        if (getType() instanceof VoidType) {
            return toRValueString(b);
        } else {
            return toRValueString(toLValueString(b).append(" = "));
        }
    }

    StringBuilder toLValueString(StringBuilder b) {
        BasicBlock block = getScheduledBlock();
        b.append('%');
        if (block != null) {
            int idx = getScheduleIndex();
            block.toString(b);
            b.append('.');
            if (idx >= 0) {
                b.append(idx);
            } else {
                b.append("??");
            }
        } else {
            b.append("??");
        }
        return b;
    }

    StringBuilder toRValueString(StringBuilder b) {
        return super.toString(b);
    }

    @Override
    public StringBuilder toReferenceString(StringBuilder b) {
        return toLValueString(b);
    }
}
