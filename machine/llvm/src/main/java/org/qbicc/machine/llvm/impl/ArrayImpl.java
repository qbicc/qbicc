package org.qbicc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.qbicc.machine.llvm.Array;
import org.qbicc.machine.llvm.IdentifiedType;
import org.qbicc.machine.llvm.LLValue;
import org.qbicc.machine.llvm.StructType;

final class ArrayImpl extends AbstractValue implements Array {
    final AbstractValue elementType;
    final ArrayList<AbstractValue> values = new ArrayList<>();

    ArrayImpl(final LLValue elementType) {
        this.elementType = (AbstractValue) elementType;
    }

    public Array item(final LLValue value) {
        values.add((AbstractValue) value);
        return this;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('[');
        Iterator<AbstractValue> iterator = values.iterator();
        boolean multiLineOutput = false;
        if (values.size() > 20) {
            // complex types are easier to read if output across multiple lines
            multiLineOutput = true;
            target.append('\n');
        }
        if (iterator.hasNext()) {
            int index = 0;
            target.append(' ');
            elementType.appendTo(target);
            target.append(' ');
            iterator.next().appendTo(target);
            while (iterator.hasNext()) {
                target.append(',');
                if (multiLineOutput) {
                    target.append(" ; " + index++ + " \n");
                }
                target.append(' ');
                elementType.appendTo(target);
                target.append(' ');
                iterator.next().appendTo(target);
            }
            if (index > 0 && multiLineOutput) {
                target.append(" ; " + index++ + " \n");
            }
        }
        target.append(' ');
        target.append(']');
        return target;
    }
}
